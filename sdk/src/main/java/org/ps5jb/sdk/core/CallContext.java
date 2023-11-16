package org.ps5jb.sdk.core;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Internal class which implements byte-code manipulation to achieve calling of the
 * functions in native libraries from Java code. This technique has been developed by theflow0.
 */
class CallContext {
    private static Pointer rtld_JVM_NativePath;
    private static Pointer libc_setjmp;
    private static Pointer libkernel___Ux86_64_setcontext;

    private native long multiNewArray(long componentType, int[] dimensions);

    static {
        initSymbols();
        installMultiNewArrayHook();
    }

    /**
     * Lookup symbols in various native libraries which are used to achieve native execution.
     *
     * @throws SdkSymbolNotFoundException If at least one of the required symbols is not found.
     * @throws SdkRuntimeException If any other error occurred during symbol initialization.
     */
    private static void initSymbols() {
        Library rtld = new Library(-2);
        rtld_JVM_NativePath = rtld.addrOf("JVM_NativePath");

        Library libc = new Library(2);
        libc_setjmp = libc.addrOf("setjmp");

        Library libkernel = new Library(0x2001);
        libkernel___Ux86_64_setcontext = libkernel.addrOf("__Ux86_64_setcontext");
    }

    /**
     * Install the hook for "multiNewArray" symbol in libjava.
     *
     * @throws SdkSymbolNotFoundException If native "multiNewArray" function could not be found in libjava.
     * @throws SdkRuntimeException If any other error occurred during "multiNewArray" hook installation.
     */
    private static void installMultiNewArrayHook() {
        boolean installed = false;

        Pointer instance = Pointer.addrOf(new CallContext());
        Pointer klass = Pointer.valueOf(instance.read8(0x08));
        Pointer methods = Pointer.valueOf(klass.read8(0x170));
        int numMethods = methods.read4();
        for (long i = 0; i < numMethods; i++) {
            Pointer method = Pointer.valueOf(methods.read8(0x08 + i * 8));
            Pointer constMethod = Pointer.valueOf(method.read8(0x08));
            Pointer constants = Pointer.valueOf(constMethod.read8(0x08));
            short nameIndex = constMethod.read2(0x2A);
            Pointer nameSymbol = Pointer.valueOf(constants.read8(0x40 + nameIndex * 8) & ~(2 - 1));
            short nameLength = nameSymbol.read2();
            String name = nameSymbol.inc(0x06).readString(new Integer(nameLength));

            if (name.equals("multiNewArray")) {
                Pointer libjava_Java_java_lang_reflect_Array_multiNewArray;
                Library libjava = new Library(0x4B);
                try {
                    libjava_Java_java_lang_reflect_Array_multiNewArray = libjava.addrOf("Java_java_lang_reflect_Array_multiNewArray");
                } catch (SdkSymbolNotFoundException e) {
                    // Library id of libjava is different between firmware versions
                    libjava = new Library(0x4A);
                    libjava_Java_java_lang_reflect_Array_multiNewArray = libjava.addrOf("Java_java_lang_reflect_Array_multiNewArray");
                }
                method.write8(0x50, libjava_Java_java_lang_reflect_Array_multiNewArray.addr());
                installed = true;
                break;
            }
        }

        if (!installed) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"installMultiNewArrayHook"));
        }
    }

    /** Memory buffer used as call stack for the native execution. */
    private Pointer callBuffer;

    /** Value needed to call "multiNewArray" function. */
    private int[] dimensions;

    /** When true, this calling context is executing a native call. It's not possible to simultaneously use the same context within two threads. */
    private boolean executing;

    /**
     * Default constructor.
     */
    CallContext() {
        super();
    }

    /**
     * Called by garbage collector when this instance is about to be disposed.
     * Note that finalization is not guaranteed and should not be relied upon
     * to close the resources. Ensure to call {@link #close()} method manually
     * when this call context is no longer needed.
     *
     * @throws Throwable If finalization failed.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Closes the resources associated with this call context.
     * Should be called before the call context is garbage collected.
     */
    void close() {
        if (this.callBuffer != null) {
            this.callBuffer.free();
            this.callBuffer = null;
        }
    }

    private void buildCallContext(Pointer contextBuf, Pointer jmpBuf, long rip, long rdi, long rsi, long rdx, long rcx, long r8, long r9) {
        long rbx = jmpBuf.read8(0x08);
        long rsp = jmpBuf.read8(0x10);
        long rbp = jmpBuf.read8(0x18);
        long r12 = jmpBuf.read8(0x20);
        long r13 = jmpBuf.read8(0x28);
        long r14 = jmpBuf.read8(0x30);
        long r15 = jmpBuf.read8(0x38);

        contextBuf.write8(0x48, rdi);
        contextBuf.write8(0x50, rsi);
        contextBuf.write8(0x58, rdx);
        contextBuf.write8(0x60, rcx);
        contextBuf.write8(0x68, r8);
        contextBuf.write8(0x70, r9);
        contextBuf.write8(0x80, rbx);
        contextBuf.write8(0x88, rbp);
        contextBuf.write8(0xA0, r12);
        contextBuf.write8(0xA8, r13);
        contextBuf.write8(0xB0, r14);
        contextBuf.write8(0xB8, r15);
        contextBuf.write8(0xE0, rip);
        contextBuf.write8(0xF8, rsp);

        contextBuf.write8(0x110, 0);
        contextBuf.write8(0x118, 0);
    }

    /**
     * Execute a native function at a given address and return the result of the execution.
     *
     * @param function Address of the function to execute.
     * @param args Arguments to pass to the function. Maximum number of arguments is 6.
     * @return Return value of the function execution.
     * @throws IllegalArgumentException If the number of arguments is greater than 6.
     *   Currently, native execution is limited to a maximum of 6 arguments.
     * @throws SdkRuntimeException If there is already a native execution in progress
     *   or for any other exception that may happen during the execution attempt.
     */
    long execute(Pointer function, long ... args) {
        if (args.length > 6) {
            throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CallContext.class,"maxCallArguments", new Integer(6), new Integer(args.length)));
        }

        if (this.callBuffer == null) {
            this.callBuffer = Pointer.malloc(0x800);
            this.dimensions = new int[] { 1 };
        }

        if (this.executing) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"nestedCall"));
        }
        this.executing = true;

        try {
            Pointer fakeClass = this.callBuffer.inc(0);
            Pointer fakeKlass = fakeClass.inc(0x100);
            Pointer fakeKlassVTable = fakeKlass.inc(0x200);
            Pointer fakeClassOop = fakeKlassVTable.inc(0x400);

            fakeClassOop.write8(fakeClass.addr());
            fakeClass.write8(0x98, fakeKlass.addr());
            fakeKlass.write4(0xC4, 0);
            fakeKlassVTable.write8(0xD8, rtld_JVM_NativePath.addr());

            fakeKlass.write8(fakeKlassVTable.addr());
            fakeKlassVTable.write8(0x158, libc_setjmp.addr());
            this.multiNewArray(fakeClassOop.addr(), this.dimensions);
            this.buildCallContext(fakeKlass, fakeKlass, function.addr(),
                    args.length > 0 ? args[0] : 0,
                    args.length > 1 ? args[1] : 0,
                    args.length > 2 ? args[2] : 0,
                    args.length > 3 ? args[3] : 0,
                    args.length > 4 ? args[4] : 0,
                    args.length > 5 ? args[5] : 0);

            fakeKlass.write8(fakeKlassVTable.addr());
            fakeKlassVTable.write8(0x158, libkernel___Ux86_64_setcontext.addr());

            long ret = this.multiNewArray(fakeClassOop.addr(), this.dimensions);
            if (ret != 0) {
                ret = Pointer.valueOf(ret).read8();
            }
            return ret;
        } catch (SdkRuntimeException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        } finally {
            this.executing = false;
        }
    }
}

package org.ps5jb.sdk.core;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.dataview.LongViewPointer;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Internal class which implements byte-code manipulation to achieve calling of the
 * functions in native libraries from Java code. This technique has been developed by theflow0.
 */
class CallContext {
    private static long rtld_JVM_NativePath;
    private static long libkernel_sigsetjmp;
    private static long libkernel___Ux86_64_setcontext;

    private native long multiNewArray(long componentType, int[] dimensions);

    static {
        try {
            initSymbols();
            installMultiNewArrayHook();
        } catch (Throwable e) {
            Status.printStackTrace(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lookup symbols in various native libraries which are used to achieve native execution.
     *
     * @throws SdkSymbolNotFoundException If at least one of the required symbols is not found.
     * @throws SdkRuntimeException If any other error occurred during symbol initialization.
     */
    private static void initSymbols() {
        Library rtld = new Library(-2);
        rtld_JVM_NativePath = rtld.addrOf("JVM_NativePath").addr();

        Library libkernel = new Library(0x2001);
        libkernel___Ux86_64_setcontext = libkernel.addrOf("__Ux86_64_setcontext").addr();
        libkernel_sigsetjmp = libkernel.addrOf("sigsetjmp").addr();
    }

    private static Pointer findMultiNewArrayAddress() {
        Pointer result = null;
        SdkSymbolNotFoundException lastException = null;

        int[] knownHandles = { 0x4A, 0x4B };
        for (int handle : knownHandles) {
            Library libjava = new Library(handle);
            try {
                result = libjava.addrOf("Java_java_lang_reflect_Array_multiNewArray");
                lastException = null;
                break;
            } catch (SdkSymbolNotFoundException e) {
                lastException = e;
            }
        }

        // Nothing found, throw the original symbol not found exception for the last known handle
        if (lastException != null) {
            throw lastException;
        }

        return result;
    }

    /**
     * Install the hook for "multiNewArray" symbol in libjava.
     *
     * @throws SdkSymbolNotFoundException If native "multiNewArray" function could not be found in libjava.
     * @throws SdkRuntimeException If any other error occurred during "multiNewArray" hook installation.
     */
    private static void installMultiNewArrayHook() {
        boolean installed = false;

        Pointer instance = Pointer.valueOf(Pointer.addrOf(new CallContext()));
        Pointer klass = Pointer.valueOf(instance.read8(0x08));
        Pointer methods = Pointer.valueOf(klass.read8(0x170));
        int numMethods = methods.read4();
        for (long i = 0; i < numMethods; i++) {
            Pointer method = Pointer.valueOf(methods.read8(0x08 + i * 8));
            Pointer constMethod = Pointer.valueOf(method.read8(0x08));
            Pointer constants = Pointer.valueOf(constMethod.read8(0x08));
            short nameIndex = constMethod.read2(0x2A);
            Pointer nameSymbol = Pointer.valueOf(constants.read8(0x40 + nameIndex * 8) & -2);
            short nameLength = nameSymbol.read2();
            String name = nameSymbol.inc(0x06).readString(new Integer(nameLength));

            if (name.equals("multiNewArray")) {
                method.write8(0x50, findMultiNewArrayAddress().addr());
                installed = true;
                break;
            }
        }

        if (!installed) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"installMultiNewArrayHook"));
        }
    }

    /** Native memory buffer used to jump into the native calls. */
    private Pointer callBuffer;
    /** Shadow Java heap buffer that is used to populate the {@link #callBuffer} data. */
    private long[] callBufferData;

    private static final int fakeClassOff = 0;
    private static final int fakeKlassOff = fakeClassOff + 0x100;
    private static final int fakeKlassVTableOff = fakeKlassOff + 0x200;
    private static final int fakeClassOopOff = fakeKlassVTableOff + 0x400;

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
            this.callBufferData = null;
        }
    }

    private void buildCallContext(long rip, long rdi, long rsi, long rdx, long rcx, long r8, long r9) {
        long rbx = callBufferData[(fakeKlassOff + 0x08) / 8];
        long rsp = callBufferData[(fakeKlassOff + 0x10) / 8];
        long rbp = callBufferData[(fakeKlassOff + 0x18) / 8];
        long r12 = callBufferData[(fakeKlassOff + 0x20) / 8];
        long r13 = callBufferData[(fakeKlassOff + 0x28) / 8];
        long r14 = callBufferData[(fakeKlassOff + 0x30) / 8];
        long r15 = callBufferData[(fakeKlassOff + 0x38) / 8];

        callBufferData[(fakeKlassOff + 0x48) / 8] = rdi;
        callBufferData[(fakeKlassOff + 0x50) / 8] = rsi;
        callBufferData[(fakeKlassOff + 0x58) / 8] = rdx;
        callBufferData[(fakeKlassOff + 0x60) / 8] = rcx;
        callBufferData[(fakeKlassOff + 0x68) / 8] = r8;
        callBufferData[(fakeKlassOff + 0x70) / 8] = r9;
        callBufferData[(fakeKlassOff + 0x80) / 8] = rbx;
        callBufferData[(fakeKlassOff + 0x88) / 8] = rbp;
        callBufferData[(fakeKlassOff + 0xA0) / 8] = r12;
        callBufferData[(fakeKlassOff + 0xA8) / 8] = r13;
        callBufferData[(fakeKlassOff + 0xB0) / 8] = r14;
        callBufferData[(fakeKlassOff + 0xB8) / 8] = r15;
        callBufferData[(fakeKlassOff + 0xE0) / 8] = rip;
        callBufferData[(fakeKlassOff + 0xF8) / 8] = rsp;
    }

    /**
     * Call "multiNewArray" hokked method to jump into a native call.
     *
     * @param rip Address to jump into.
     * @return Return value from the native call execution.
     */
    private long invokeMultiNewArrayHook(long rip) {
        callBufferData[fakeKlassOff / 8] = callBuffer.addr() + fakeKlassVTableOff;
        callBufferData[(fakeKlassVTableOff + 0x158) / 8] = rip;

        return this.multiNewArray(callBuffer.addr() + fakeClassOopOff, this.dimensions);
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

        if (this.executing) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"nestedCall"));
        }
        this.executing = true;

        // On the first call, initialize internal structures
        if (this.callBuffer == null) {
            final int callBufferSize = 0x800;
            LongViewPointer longViewCallBuffer = new LongViewPointer(callBufferSize / 8);
            this.callBuffer = longViewCallBuffer;

            this.callBufferData = longViewCallBuffer.dataView();
            this.callBufferData[fakeClassOopOff / 8] = this.callBuffer.addr();
            this.callBufferData[(fakeClassOff + 0x98) / 8] = this.callBuffer.addr() + fakeKlassOff;
            this.callBufferData[(fakeKlassVTableOff + 0xD8) / 8] = rtld_JVM_NativePath;

            this.dimensions = new int[] { 1 };
        }

        try {
            this.callBufferData[(fakeKlassOff + 0xC0) / 8] = 0;
            this.invokeMultiNewArrayHook(libkernel_sigsetjmp + 0x23);

            this.buildCallContext(function.addr(),
                    args.length > 0 ? args[0] : 0,
                    args.length > 1 ? args[1] : 0,
                    args.length > 2 ? args[2] : 0,
                    args.length > 3 ? args[3] : 0,
                    args.length > 4 ? args[4] : 0,
                    args.length > 5 ? args[5] : 0);
            long ret = this.invokeMultiNewArrayHook(libkernel___Ux86_64_setcontext + 0x39);

            if (ret != 0) {
                ret = Pointer.valueOf(ret).read8();
            }
            return ret;
        } catch (SdkRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        } finally {
            this.executing = false;
        }
    }
}

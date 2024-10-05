package org.ps5jb.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * Class managing capability of SDK to read/write the kernel memory.
 */
public final class KernelReadWrite {
    private static KernelAccessor kernelAccessor;

    /** Kernel accessor state, serialized by calling {@link #saveAccessor()} */
    private static byte[] kernelAccessorState;

    /** Custom object input stream which can use a specific class loader to find the class */
    private static class ClassLoaderObjectInputStream extends ObjectInputStream {
        ClassLoader classLoader;

        /**
         * Constructor of the class
         *
         * @param in Stream from which to read the objects.
         * @param classLoader Classloader to use for resolving classes.
         * @throws IOException An exception occurred in the underlying stream.
         */
        private ClassLoaderObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            Class result = null;

            if (classLoader != null) {
                String className = desc.getName();
                try {
                    result = Class.forName(className, false, classLoader);
                } catch (ClassNotFoundException ex) {
                    Status.printStackTrace("Could not resolve the class " + className + " using classloader " + classLoader, ex);
                }
            }

            if (result == null) {
                result = super.resolveClass(desc);
            }

            return result;
        }
    }

    /**
     * Default constructor
     */
    private KernelReadWrite() {
    }

    /**
     * Register a global instance of a kernel accessor, responsible for
     * reading and writing kernel memory.
     *
     * @param kernelAccessor New accessor instance.
     */
    public static synchronized void setAccessor(KernelAccessor kernelAccessor) {
        KernelReadWrite.kernelAccessor = kernelAccessor;
    }

    /**
     * Retrieve a global instance of a kernel accessor. May be null
     * if none are installed.
     *
     * @return Instance of a kernel accessor or null.
     */
    public static synchronized KernelAccessor getAccessor() {
        return KernelReadWrite.kernelAccessor;
    }

    /**
     * Saves the current state of a global kernel accessor instance internally and
     * sets it to <code>null</code>.
     *
     * @return True if state was successfully saved.
     */
    public static synchronized boolean saveAccessor() {
        if (kernelAccessor != null) {
            try {
                ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream outStr = new ObjectOutputStream(outBytes);
                    try {
                        outStr.writeObject(kernelAccessor);
                        outStr.flush();
                        kernelAccessorState = outBytes.toByteArray();
                    } finally {
                        outStr.close();
                    }
                } finally {
                    outBytes.close();
                }
            } catch (IOException | RuntimeException | Error e) {
                Status.printStackTrace("Exception occurred while saving the kernel accessor state", e);
            }
        } else {
            kernelAccessorState = null;
        }

        // If error occurred on close, state is correctly serialized so consider it a success.
        boolean result = kernelAccessorState != null || kernelAccessor == null;
        if (kernelAccessorState != null) {
            kernelAccessor = null;
        }

        return result;
    }

    /**
     * Restores kernel accessor instance from a previously saved state.
     *
     * @param classLoader ClassLoader to use to find the kernel accessor class
     * @return True if kernel accessor was activated. False if there was no previously saved
     *   accessor or if it could not be activated.
     */
    public static synchronized boolean restoreAccessor(ClassLoader classLoader) {
        if (kernelAccessorState != null) {
            try {
                ByteArrayInputStream inBytes = new ByteArrayInputStream(kernelAccessorState);
                try {
                    ClassLoaderObjectInputStream inStr = new ClassLoaderObjectInputStream(inBytes, classLoader);
                    try {
                        kernelAccessor = (KernelAccessor) inStr.readObject();
                    } finally {
                        inStr.close();
                    }
                } finally {
                    inBytes.close();
                }
            } catch (ClassNotFoundException | IOException | RuntimeException | Error e) {
                Status.printStackTrace("Exception occurred while restoring the kernel accessor", e);
            }
        }

        boolean result = kernelAccessor != null;
        if (result) {
            kernelAccessorState = null;
        }

        return result;
    }
}

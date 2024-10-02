package org.ps5jb.sdk.core.kernel;

/**
 * Class managing capability of SDK to read/write the kernel memory.
 */
public final class KernelReadWrite {
    // Note: is "volatile" enough? Probably better to do synchronized methods.
    private static volatile KernelAccessor kernelAccessor;

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
    public static void setAccessor(KernelAccessor kernelAccessor) {
        KernelReadWrite.kernelAccessor = kernelAccessor;
    }

    /**
     * Retrieve a global instance of a kernel accessor. May be null
     * if none are installed.
     *
     * @return Instance of a kernel accessor or null.
     */
    public static KernelAccessor getAccessor() {
        return KernelReadWrite.kernelAccessor;
    }
}

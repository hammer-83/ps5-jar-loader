package org.ps5jb.sdk.core.kernel;

/**
 * Interface for accessing Kernel memory. Various exploits able to
 * do so should implement this interface.
 */
public interface KernelAccessor {
    byte read1(long kernelAddress);
    short read2(long kernelAddress);
    int read4(long kernelAddress);
    long read8(long kernelAddress);

    void write1(long kernelAddress, byte value);
    void write2(long kernelAddress, short value);
    void write4(long kernelAddress, int value);
    void write8(long kernelAddress, long value);
}

package org.ps5jb.sdk.core.kernel;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.AbstractPointer;

/**
 * Abstraction over memory pointer operations in kernel-space.
 * This class is only usable if a global kernel accessor has been installed
 * by calling {@link KernelReadWrite#setAccessor(KernelAccessor)}.
 */
public class KernelPointer extends AbstractPointer {
    private static final long serialVersionUID = 3445279334363239500L;

    /** Start of kernel address space. See machine/vmparam.h */
    private static final long KERNEL_ADDR_MASK = 0xFFFF800000000000L;

    /** Flag which makes sure that range of kernel space is validated prior to accessing the memory. */
    private boolean rangeValidated = false;

    /**
     * Returns a pointer instance equivalent to the given native memory address.
     *
     * @param addr Address to convert to a Pointer instance.
     * @return Pointer instance representing the given native memory address.
     */
    public static KernelPointer valueOf(long addr) {
        return addr == 0 ? NULL : new KernelPointer(addr);
    }

    /**
     * Validates that the given kernel pointer has the correct kernel-space range.
     *
     * @param pointer Pointer to validate.
     * @return Same pointer instance.
     * @throws IllegalAccessError If the pointer is invalid (including NULL).
     */
    public static KernelPointer validRange(KernelPointer pointer) {
        if ((((pointer.addr() & KERNEL_ADDR_MASK) != KERNEL_ADDR_MASK) || pointer.addr() == -1)) {
            throw new IllegalAccessError(pointer.toString());
        }
        return pointer;
    }

    /** Static constant for NULL pointer. */
    public static final KernelPointer NULL = new KernelPointer(0);

    public KernelPointer(long addr) {
        super(addr);
    }

    public KernelPointer(long addr, Long size) {
        super(addr, size);
    }

    /**
     * Make sure the current pointer has a valid kernel space range.
     */
    protected void checkRange() {
        if (!rangeValidated) {
            validRange(this);
            rangeValidated = true;
        }
    }

    @Override
    public byte read1(long offset) {
        checkRange();
        return super.read1(offset);
    }

    @Override
    public short read2(long offset) {
        checkRange();
        return super.read2(offset);
    }

    @Override
    public int read4(long offset) {
        checkRange();
        return super.read4(offset);
    }

    @Override
    public long read8(long offset) {
        checkRange();
        return super.read8(offset);
    }

    @Override
    public void read(long offset, byte[] value, int valueOffset, int size) {
        checkRange();

        KernelAccessor ka = KernelReadWrite.getAccessor();
        if (ka instanceof KernelAccessorIPv6) {
            // When using IPv6 based accessor, use a more efficient read method
            // rather that writing 8-bytes at a time.
            KernelAccessorIPv6 kaIpv6 = (KernelAccessorIPv6) ka;
            kaIpv6.read(this.addr + offset, value, valueOffset, size);
        } else {
            super.read(offset, value, valueOffset, size);
        }
    }

    @Override
    public void write1(long offset, byte value) {
        checkRange();
        super.write1(offset, value);
    }

    @Override
    public void write2(long offset, short value) {
        checkRange();
        super.write2(offset, value);
    }

    @Override
    public void write4(long offset, int value) {
        checkRange();
        super.write4(offset, value);
    }

    @Override
    public void write8(long offset, long value) {
        checkRange();
        super.write8(offset, value);
    }

    @Override
    public void write(long offset, byte[] value, int valueOffset, int count) {
        checkRange();

        KernelAccessor ka = KernelReadWrite.getAccessor();
        if (ka instanceof KernelAccessorIPv6) {
            // When using IPv6 based accessor, use a more efficient write method
            // rather that writing 8-bytes at a time.
            KernelAccessorIPv6 kaIpv6 = (KernelAccessorIPv6) ka;
            kaIpv6.write(this.addr + offset, value, valueOffset, count);
        } else {
            super.write(offset, value, valueOffset, count);
        }
    }

    @Override
    protected byte read1impl(long offset) {
        return KernelReadWrite.getAccessor().read1(this.addr + offset);
    }

    @Override
    protected short read2impl(long offset) {
        return KernelReadWrite.getAccessor().read2(this.addr + offset);
    }

    @Override
    protected int read4impl(long offset) {
        return KernelReadWrite.getAccessor().read4(this.addr + offset);
    }

    @Override
    protected long read8impl(long offset) {
        return KernelReadWrite.getAccessor().read8(this.addr + offset);
    }

    @Override
    protected void write1impl(long offset, byte value) {
        KernelReadWrite.getAccessor().write1(this.addr + offset, value);
    }

    @Override
    protected void write2impl(long offset, short value) {
        KernelReadWrite.getAccessor().write2(this.addr + offset, value);
    }

    @Override
    protected void write4impl(long offset, int value) {
        KernelReadWrite.getAccessor().write4(this.addr + offset, value);
    }

    @Override
    protected void write8impl(long offset, long value) {
        KernelReadWrite.getAccessor().write8(this.addr + offset, value);
    }

    /**
     * Copies values in kernel memory associated with this pointer to a pointer specified by <code>dest</code>.
     *
     * @param dest Pointer to copy the data to. The data will always be copied starting at offset 0 in <code>dest</code>.
     * @param offset Offset in this memory to read the data from.
     * @param size Size of data to copy.
     * @throws IndexOutOfBoundsException If the read or the write beyond one of the two pointers' sizes occurs.
     */
    public void copyTo(KernelPointer dest, long offset, int size) {
        byte[] data = new byte[size];
        read(offset, data, 0, size);
        dest.write(0, data, 0, size);
    }

    /**
     * Increment the pointer by a given offset. The <code>delta</code> parameter may be negative.
     *
     * @param delta Offset from the current address of this pointer.
     * @return New pointer instance whose size is unknown and whose address is the address of
     *   this pointer shifted by <code>delta</code> bytes.
     */
    public KernelPointer inc(long delta) {
        // Size is intentionally left unknown
        return KernelPointer.valueOf(this.addr + delta);
    }
}

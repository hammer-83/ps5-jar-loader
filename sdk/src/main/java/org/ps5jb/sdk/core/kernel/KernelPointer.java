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

    @Override
    public byte read1(long offset) {
        overflow(validRange(this), offset, 1);
        return KernelReadWrite.getAccessor().read1(this.addr + offset);
    }

    @Override
    public short read2(long offset) {
        overflow(validRange(this), offset, 2);
        return KernelReadWrite.getAccessor().read2(this.addr + offset);
    }

    @Override
    public int read4(long offset) {
        overflow(validRange(this), offset, 4);
        return KernelReadWrite.getAccessor().read4(this.addr + offset);
    }

    @Override
    public long read8(long offset) {
        overflow(validRange(this), offset, 8);
        return KernelReadWrite.getAccessor().read8(this.addr + offset);
    }

    @Override
    public void read(long offset, byte[] value, int valueOffset, int size) {
        overflow(validRange(this), offset, size);

        // TODO: This can be implemented more efficiently
        final KernelAccessor accessor = KernelReadWrite.getAccessor();
        for (int i = 0; i < size; ++i) {
            value[valueOffset + i] = accessor.read1(this.addr + offset + i);
        }
    }

    @Override
    public void write1(long offset, byte value) {
        overflow(validRange(this), offset, 1);
        KernelReadWrite.getAccessor().write1(this.addr + offset, value);
    }

    @Override
    public void write2(long offset, short value) {
        overflow(validRange(this), offset, 2);
        KernelReadWrite.getAccessor().write2(this.addr + offset, value);
    }

    @Override
    public void write4(long offset, int value) {
        overflow(validRange(this), offset, 4);
        KernelReadWrite.getAccessor().write4(this.addr + offset, value);
    }

    @Override
    public void write8(long offset, long value) {
        overflow(validRange(this), offset, 8);
        KernelReadWrite.getAccessor().write8(this.addr + offset, value);
    }

    @Override
    public void write(long offset, byte[] value, int valueOffset, int count) {
        overflow(validRange(this), offset, count);

        // TODO: This can be implemented more efficiently
        final KernelAccessor accessor = KernelReadWrite.getAccessor();
        for (int i = 0; i < count - valueOffset; ++i) {
            accessor.write1(this.addr + offset + i, value[valueOffset + i]);
        }
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

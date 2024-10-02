package org.ps5jb.sdk.core;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.PrivilegedActionException;

import jdk.internal.misc.Unsafe;

/**
 * Abstraction over memory pointer operations in user-space.
 */
public class Pointer extends AbstractPointer {
    /**
     * Helper class to obtain an instance of Unsafe in a thread-safe manner.
     */
    private static class UnsafeHolder {
        private static final Unsafe unsafe;
        private static final long longValueOffset;

        static {
            try {
                OpenModuleAction.execute("jdk.internal.misc.Unsafe");

                unsafe = Unsafe.getUnsafe();
                longValueOffset = unsafe.objectFieldOffset(Long.class.getDeclaredField("value"));
            } catch (PrivilegedActionException e) {
                if (e.getException() instanceof InvocationTargetException) {
                    throw new SdkRuntimeException(((InvocationTargetException) e.getException()).getTargetException());
                }
                throw new SdkRuntimeException(e.getException());
            } catch (NoSuchFieldException | RuntimeException | Error e) {
                throw new SdkRuntimeException(e);
            }
        }
    }

    /**
     * Get the instance of {@link Unsafe} class.
     *
     * @return Singleton instance of <code>Unsafe</code> class.
     */
    protected static Unsafe getUnsafe() {
        return UnsafeHolder.unsafe;
    }

    /**
     * Return the field offset for the "value" field of the <code>Long</code> class.
     * Used by native call framework to retrieve native memory addresses of Java objects.
     *
     * @return Field offset of <code>Long#value</code>.
     */
    private static long getLongValueOffset() {
        return UnsafeHolder.longValueOffset;
    }

    /**
     * Allocate the native memory of a given size and return a pointer to it.
     *
     * @param size Allocation size.
     * @return Pointer to the allocated memory.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer malloc(long size) {
        return new Pointer(getUnsafe().allocateMemory(size), new Long(size));
    }

    /**
     *  Allocate the native memory of a given size, initialize it with zeroes
     *  and return a pointer to it.
     *
     * @param size Allocation size.
     * @return Pointer to the allocated memory.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer calloc(long size) {
        Pointer result = malloc(size);
        try {
            int i = 0;
            while ((i + 8) <= size) {
                result.write8(i, 0);
                i += 8;
            }
            while (i < size) {
                result.write1(i, (byte) 0);
                ++i;
            }
        } catch (RuntimeException | Error e) {
            result.free();
            throw e;
        }
        return result;
    }

    /**
     * Allocate a new native memory buffer and copy the contents
     * of the given Java string into it, converting characters
     * to bytes using the default system charset. The resulting
     * native string is null-terminated.
     *
     * @param string String to convert to a native null-terminated string.
     * @return Pointer to the allocated buffer.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer fromString(String string) {
        return fromString(string, Charset.defaultCharset().name());
    }

    /**
     * Allocate a new native memory buffer and copy the contents
     * of the given Java string into it, converting characters
     * to bytes using the specified charset. The resulting
     * native string is null-terminated.
     *
     * @param string String to convert to a native null-terminated string.
     * @param charset Character set to use to convert from native bytes to a Java string.
     * @return Pointer to the allocated buffer.
     * @throws OutOfMemoryError if the allocation is refused by the system.
     */
    public static Pointer fromString(String string, String charset) {
        byte[] stringBuffer;
        try {
            stringBuffer = string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new SdkRuntimeException(e);
        }

        Pointer result = malloc(stringBuffer.length + 1);
        result.write(stringBuffer);
        result.write1(stringBuffer.length, (byte) 0);
        return result;
    }

    /**
     * Return the native address of a given object.
     *
     * @param object Object whose native address to retrieve.
     * @return Pointer to the native address of the given object.
     */
    static Pointer addrOf(Object object) {
        Long val = new Long(0);
        getUnsafe().putObject(val, getLongValueOffset(), object);
        return Pointer.valueOf(getUnsafe().getLong(val, getLongValueOffset()));
    }

    /**
     * Returns a pointer instance equivalent to the given native memory address.
     *
     * @param addr Address to convert to a Pointer instance.
     * @return Pointer instance representing the given native memory address.
     */
    public static Pointer valueOf(long addr) {
        return addr == 0 ? NULL : new Pointer(addr);
    }

    /** Static constant for NULL pointer. */
    public static final Pointer NULL = new Pointer(0);

    public Pointer(long addr) {
        super(addr);
    }

    public Pointer(long addr, Long size) {
        super(addr, size);
    }

    @Override
    public byte read1(long offset) {
        overflow(this, offset, 1);
        return getUnsafe().getByte(this.addr + offset);
    }

    @Override
    public short read2(long offset) {
        overflow(this, offset, 2);
        return getUnsafe().getShort(this.addr + offset);
    }

    @Override
    public int read4(long offset) {
        overflow(this, offset, 4);
        return getUnsafe().getInt(this.addr + offset);
    }

    @Override
    public long read8(long offset) {
        overflow(this, offset, 8);
        return getUnsafe().getLong(this.addr + offset);
    }

    @Override
    public void read(long offset, byte[] value, int valueOffset, int size) {
        overflow(this, offset, size);

        // TODO: This can be implemented more efficiently
        for (int i = 0; i < size; ++i) {
            value[valueOffset + i] = getUnsafe().getByte(this.addr + offset + i);
        }
    }

    @Override
    public void write1(long offset, byte value) {
        overflow(this, offset, 1);
        getUnsafe().putByte(this.addr + offset, value);
    }

    @Override
    public void write2(long offset, short value) {
        overflow(this, offset, 2);
        getUnsafe().putShort(this.addr + offset, value);
    }

    @Override
    public void write4(long offset, int value) {
        overflow(this, offset, 4);
        getUnsafe().putInt(this.addr + offset, value);
    }

    @Override
    public void write8(long offset, long value) {
        overflow(this, offset, 8);
        getUnsafe().putLong(this.addr + offset, value);
    }

    @Override
    public void write(long offset, byte[] value, int valueOffset, int count) {
        overflow(this, offset, count);

        // TODO: This can be implemented more efficiently
        for (int i = 0; i < count - valueOffset; ++i) {
            getUnsafe().putByte(this.addr + offset + i, value[valueOffset + i]);
        }
    }

    /**
     * Copies values in native memory associated with this pointer to a pointer specified by <code>dest</code>.
     *
     * @param dest Pointer to copy the data to. The data will always be copied starting at offset 0 in <code>dest</code>.
     * @param offset Offset in this memory to read the data from.
     * @param size Size of data to copy.
     * @throws IndexOutOfBoundsException If the read or the write beyond one of the two pointers' sizes occurs.
     */
    public void copyTo(Pointer dest, long offset, int size) {
        overflow(this, offset, size);
        overflow(dest, 0, size);

        byte[] data = new byte[size];
        read(offset, data, 0, size);
        dest.write(0, data, 0, size);
    }

    /**
     * Free the native memory associated with this pointer.
     */
    public void free() {
        getUnsafe().freeMemory(this.addr);
        this.addr = 0;
        this.size = null;
    }

    /**
     * Increment the pointer by a given offset. The <code>delta</code> parameter may be negative.
     *
     * @param delta Offset from the current address of this pointer.
     * @return New pointer instance whose size is unknown and whose address is the address of
     *   this pointer shifted by <code>delta</code> bytes.
     */
    public Pointer inc(long delta) {
        // Size is intentionally left unknown
        return Pointer.valueOf(this.addr + delta);
    }
}

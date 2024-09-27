package org.ps5jb.sdk.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.PrivilegedActionException;

import jdk.internal.misc.Unsafe;

/**
 * Abstraction over memory pointer operations.
 */
public class Pointer {
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
     * Wrap the given pointer in a non-null check. Returns the same pointer if it is not NULL.
     *
     * @param pointer Pointer to verify.
     * @param errorMessage Error message assigned to the exception if the pointer is NULL.
     * @return Same <code>pointer</code> value if it is not NULL.
     * @throws NullPointerException If the input pointer is NULL or points to the address 0.
     */
    public static Pointer nonNull(Pointer pointer, String errorMessage) {
        if (pointer == null || pointer.addr() == 0) {
            throw new NullPointerException(errorMessage);
        }
        return pointer;
    }

    /**
     * Check that the pointer operation will result in an overflow.
     *
     * @param pointer Pointer to verify.
     * @param offset Offset at which the pointer operation is started.
     * @param size Size of the pointer operation.
     * @throws IndexOutOfBoundsException If the operation is out of bounds.
     */
    public static void overflow(Pointer pointer, long offset, long size) {
        // All overflow checks are disabled when size is unknown
        if (pointer.size != null) {
            if (offset < 0) {
                throw new IndexOutOfBoundsException(Long.toString(offset));
            }

            if (((offset + size) > pointer.size.longValue())) {
                throw new IndexOutOfBoundsException(Long.toString(offset + size));
            }
        }
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

    /** Native memory address pointed to by this instance. */
    private long addr;

    /** Size of the memory pointed to by this instance. May be null if the size is unknown. */
    private Long size;

    /**
     * Constructor of a pointer without a known size.
     *
     * @param addr Memory address of the pointer.
     */
    public Pointer(long addr) {
        this(addr, null);
    }

    /**
     * Constructor of a pointer where the size is known.
     *
     * @param addr Memory address of the pointer.
     * @param size Size of the memory.
     */
    public Pointer(long addr, Long size) {
        this.addr = addr;
        this.size = size;
    }

    /**
     * Read 1 byte at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public byte read1(long offset) {
        overflow(this, offset, 1);
        return getUnsafe().getByte(this.addr + offset);
    }

    /**
     * Read 1 byte from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public byte read1() {
        return read1(0);
    }

    /**
     * Read 2 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public short read2(long offset) {
        overflow(this, offset, 2);
        return getUnsafe().getShort(this.addr + offset);
    }

    /**
     * Read 2 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public short read2() {
        return read2(0);
    }

    /**
     * Read 4 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public int read4(long offset) {
        overflow(this, offset, 4);
        return getUnsafe().getInt(this.addr + offset);
    }

    /**
     * Read 4 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public int read4() {
        return read4(0);
    }

    /**
     * Read 8 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public long read8(long offset) {
        overflow(this, offset, 8);
        return getUnsafe().getLong(this.addr + offset);
    }

    /**
     * Read 8 bytes from the address pointed to by this pointer instance.
     *
     * @return Value read from the memory.
     */
    public long read8() {
        return read8(0);
    }

    /**
     * Read the given number of bytes from the address pointed to by this pointer instance.
     *
     * @param size Number of bytes to read.
     * @return Value read from the memory as an array of bytes.
     */
    public byte[] read(int size) {
        byte[] result = new byte[size];
        read(0, result, 0, size);
        return result;
    }

    /**
     * Read the given number of bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Buffer to read the value into.
     * @param valueOffset Offset in the buffer where to place the read value.
     * @param size Number of bytes to read.
     * @throws IndexOutOfBoundsException If the buffer is not large enough to hold the value
     *   of the specified size.
     */
    public void read(long offset, byte[] value, int valueOffset, int size) {
        overflow(this, offset, size);
        for (int i = 0; i < size; ++i) {
            value[valueOffset + i] = read1(offset + i);
        }
    }

    /**
     * Read the data at the specified offset from the pointer assuming it is a native
     * null-terminated string.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param maxLength Maximum number of bytes to read. If <code>null</code>,
     *   the memory will be read until the null character is encountered.
     * @param charset Charset to use to convert the native string into a Java
     *   string.
     * @return Value of the memory as a Java string.
     * @throws SdkRuntimeException If the string could not be read, for example
     *   if the bytes cannot be converted to the target charset.
     */
    public String readString(long offset, Integer maxLength, String charset) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try {
                long curSize = 0;
                byte c = read1(offset);
                while (c != 0 && (maxLength == null || maxLength.intValue() > curSize)) {
                    buf.write(c);
                    ++curSize;
                    c = read1(offset + curSize);
                }
            } finally {
                buf.close();
            }
            return buf.toString(charset);
        } catch (IOException e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Read the current value of the pointer assuming the data is a native
     * null-terminated string. The bytes in the native memory are converted to
     * Java string using the system default charset.
     *
     * @param maxLength Maximum number of bytes to read. If <code>null</code>,
     *   the memory will be read until the null character is encountered.
     * @return Value of the memory as a Java string.
     */
    public String readString(Integer maxLength) {
        return readString(0, maxLength, Charset.defaultCharset().name());
    }

    /**
     * Write 1 byte at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write1(long offset, byte value) {
        overflow(this, offset, 1);
        getUnsafe().putByte(this.addr + offset, value);
    }

    /**
     * Write 1 byte to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write1(byte value) {
        write1(0, value);
    }

    /**
     * Write 2 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write2(long offset, short value) {
        overflow(this, offset, 2);
        getUnsafe().putShort(this.addr + offset, value);
    }

    /**
     * Write 2 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write2(short value) {
        write2(0, value);
    }

    /**
     * Write 4 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write4(long offset, int value) {
        overflow(this, offset, 4);
        getUnsafe().putInt(this.addr + offset, value);
    }

    /**
     * Write 4 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write4(int value) {
        write4(0, value);
    }

    /**
     * Write 8 bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Value to write.
     */
    public void write8(long offset, long value) {
        overflow(this, offset, 8);
        getUnsafe().putLong(this.addr + offset, value);
    }

    /**
     * Write 8 bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write8(long value) {
        write8(0, value);
    }

    /**
     * Write the given number of bytes to the address pointed to by this pointer instance.
     *
     * @param value Value to write.
     */
    public void write(byte[] value) {
        this.write(0, value, 0, value.length);
    }

    /**
     * Write the given number of bytes at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param value Buffer to write.
     * @param valueOffset Offset in the buffer from which to start writing.
     * @param count Number of bytes to write.
     * @throws IndexOutOfBoundsException If the buffer or the native memory
     *   are not large enough for the given values of <code>offset</code>,
     *   <code>valueOffset</code> and <code>count</code>.
     */
    public void write(long offset, byte[] value, int valueOffset, int count) {
        overflow(this, offset, count);
        for (int i = 0; i < count - valueOffset; ++i) {
            getUnsafe().putByte(this.addr + offset + i, value[valueOffset + i]);
        }
    }

    /**
     * Write a given Java string at the specified offset from the pointer as
     * a native null-terminated string.
     *
     * @param offset Offset relative to {@link #addr}.
     * @param string String value to write.
     * @param charset Charset to use to convert the native string into a Java
     *   string.
     * @throws SdkRuntimeException If the string could not be written, for example
     *   if the bytes cannot be converted to the target charset.
     * @throws IndexOutOfBoundsException If the write beyond the pointer size occurs.
     */
    public void writeString(long offset, String string, String charset) {
        byte[] stringBuffer;
        try {
            stringBuffer = string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new SdkRuntimeException(e);
        }

        write(offset, stringBuffer, 0, stringBuffer.length);
        write1(offset + stringBuffer.length, (byte) 0);
    }

    public void writeString(String string) {
        writeString(0, string, Charset.defaultCharset().name());
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
     * Get the native memory address of this pointer.
     *
     * @return Native memory address of this pointer.
     */
    public long addr() {
        return this.addr;
    }

    /**
     * Get the size of the allocated native memory pointed to by this instance.
     *
     * @return Size of the memory pointed to by this pointer. May be null if the size is unknown.
     */
    public Long size() {
        return this.size;
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

    /**
     * Compares the address of this pointer with the address of another pointer.
     * Note that size does not play a role in equality of two pointers.
     *
     * @param obj Other pointer to compare this pointer to.
     *   If this object is not a pointer, the comparison returns <code>false</code>.
     * @return True if <code>obj</code> is a pointer and its address is the same
     *   as this instance's address.
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof Pointer) {
            // Size is not considered when evaluating equality
            result = ((Pointer) obj).addr == this.addr;
        }
        return result;
    }

    /**
     * Computes a hashcode for this pointer.
     *
     * @return This pointer's hashcode.
     */
    @Override
    public int hashCode() {
        return (new Long(this.addr)).hashCode();
    }
}

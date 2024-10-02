package org.ps5jb.sdk.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Root parent for any class that implements a pointer to a memory.
 */
public abstract class AbstractPointer {
    /**
     * Wrap the given pointer in a non-null check. Returns the same pointer if it is not NULL.
     *
     * @param pointer Pointer to verify.
     * @param errorMessage Error message assigned to the exception if the pointer is NULL.
     * @return Same <code>pointer</code> value if it is not NULL.
     * @throws NullPointerException If the input pointer is NULL or points to the address 0.
     */
    public static AbstractPointer nonNull(AbstractPointer pointer, String errorMessage) {
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
    public static void overflow(AbstractPointer pointer, long offset, long size) {
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

    /** Native memory address pointed to by this instance. */
    protected long addr;

    /** Size of the memory pointed to by this instance. May be null if the size is unknown. */
    protected Long size;

    /**
     * Constructor of a pointer without a known size.
     *
     * @param addr Memory address of the pointer.
     */
    protected AbstractPointer(long addr) {
        this(addr, null);
    }

    /**
     * Constructor of a pointer where the size is known.
     *
     * @param addr Memory address of the pointer.
     * @param size Size of the memory.
     */
    protected AbstractPointer(long addr, Long size) {
        this.addr = addr;
        this.size = size;
    }

    /**
     * Read 1 byte at the specified offset from the pointer.
     *
     * @param offset Offset relative to {@link #addr}.
     * @return Value read from the memory.
     */
    public abstract byte read1(long offset);

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
    public abstract short read2(long offset);

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
    public abstract int read4(long offset);

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
    public abstract long read8(long offset);

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
        // TODO: This can be implemented more efficiently
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
    public abstract void write1(long offset, byte value);

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
    public abstract void write2(long offset, short value);

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
    public abstract void write4(long offset, int value);

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
    public abstract void write8(long offset, long value);

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
        // TODO: This can be implemented more efficiently
        for (int i = 0; i < count - valueOffset; ++i) {
            write1(this.addr + offset + i, value[valueOffset + i]);
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
        if (obj instanceof AbstractPointer) {
            // Size is not considered when evaluating equality
            result = ((AbstractPointer) obj).addr == this.addr;
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

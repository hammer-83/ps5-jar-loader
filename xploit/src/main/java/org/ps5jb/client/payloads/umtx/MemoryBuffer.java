package org.ps5jb.client.payloads.umtx;

import java.util.Arrays;

import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;

public class MemoryBuffer {
    private final AbstractPointer ptr;

    public MemoryBuffer(AbstractPointer ptr, long size) {
        if (ptr.size() == null || ptr.size().longValue() != size) {
            if (ptr instanceof Pointer) {
                this.ptr = new Pointer(ptr.addr(), new Long(size));
            } else {
                this.ptr = new KernelPointer(ptr.addr(), new Long(size));
            }
        } else {
            this.ptr = ptr;
        }
    }

    public long getAddr() {
        return this.ptr.addr();
    }

    public long getSize() {
        return this.ptr.size();
    }

    public int read8(long offset) {
        return this.ptr.read1(offset);
    }

    public long read64(long offset) {
        return this.ptr.read8(offset);
    }

    public void dump() {
        final long count = getSize();

        for (int j = 0; j < count; j += 0x10) {
            final Pointer offsetPtr = Pointer.valueOf(j);
            if ((j + 0x10) <= count) {
                final long value1 = read64(j);
                final long value2 = read64(j + 8);

                String value1str = Long.toHexString(value1);
                while (value1str.length() < 0x10) {
                    value1str = "0" + value1str;
                }

                String value2str = Long.toHexString(value2);
                while (value2str.length() < 0x10) {
                    value2str = "0" + value2str;
                }

                DebugStatus.info(offsetPtr + ": " + value1str + " " + value2str);
            }
            else {
                // TODO: Implement dumping of last partial line
                DebugStatus.info(offsetPtr + ": tail omitted");
            }
        }
    }

    public long find(AbstractPointer pattern, int size) {
        byte[] val = pattern.read(size);

        byte[] nextVal = new byte[size];
        long curOffset = 0;
        while ((curOffset + size) <= getSize()) {
            this.ptr.read(curOffset, nextVal, 0, size);
            if (Arrays.equals(val, nextVal)) {
                return curOffset;
            }
            curOffset++;
        }

        return -1;
    }
}

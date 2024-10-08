package org.ps5jb.client.payloads.umtx.kernel;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;

public class MemoryDumper {
    public static void dump(AbstractPointer buf, long size) {
        StringBuffer sb = new StringBuffer(110);
        for (int j = 0; j < size; j += 0x10) {
            final Pointer offsetPtr = Pointer.valueOf(j);
            sb.append(offsetPtr);
            sb.append(":   ");

            for (int i = 0; i < 2; ++i) {
                if ((j + i + 0x8) <= size) {
                    long value = buf.read8(j + i * 8);
                    for (int k = 0; k < 8; ++k) {
                        String hex = Long.toHexString((value >> (k * 8)) & 0xFF);
                        if (k != 0) {
                            sb.append(" ");
                        } else if (i == 1) {
                            sb.append("      ");
                        }
                        if (hex.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(hex);
                    }
                } else {
                    for (int k = 0; (j + i + k) < size; ++k) {
                        byte val = buf.read1(j + i + k);
                        String hex = Integer.toHexString(val & 0xFF);
                        if (k != 0) {
                            sb.append(" ");
                        } else if (i == 1) {
                            sb.append("      ");
                        }
                        if (hex.length() == 1) {
                            sb.append("0");
                        }
                        sb.append(hex);
                    }
                }
            }

            Status.println(sb.toString());
            sb.setLength(0);
        }
    }
}

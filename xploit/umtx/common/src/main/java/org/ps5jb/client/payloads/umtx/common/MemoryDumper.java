package org.ps5jb.client.payloads.umtx.common;

import org.ps5jb.sdk.core.AbstractPointer;
import org.ps5jb.sdk.core.Pointer;

public class MemoryDumper {
    public static void dump(AbstractPointer buf, long size, boolean relative) {
        StringBuffer sb = new StringBuffer(110);
        for (int j = 0; j < size; j += 0x10) {
            sb.append(AbstractPointer.toString(relative ? j : buf.addr() + j));
            sb.append(":   ");

            for (int i = 0; i < 2; ++i) {
                if ((j + i * 8 + 0x8) <= size) {
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
                    for (int k = 0; (j + i * 8 + k) < size; ++k) {
                        byte val = buf.read1(j + i * 8 + k);
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

            DebugStatus.info(sb.toString());
            sb.setLength(0);
        }
    }
}

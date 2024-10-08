package org.ps5jb.sdk.core.kernel;

import java.text.MessageFormat;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

/**
 * Class which is able to return various interesting offsets in kernel based on the console firmware version.
 * Note that currently not all firmware versions are supported.
 * Specter's Webkit UMTX repo was used as source of these offsets.
 */
public class KernelOffsets {
    // Kernel text-relative offsets
    public final long OFFSET_KERNEL_DATA;

    // Kernel data-relative offsets
    public final long OFFSET_KERNEL_DATA_BASE_ALLPROC;
    public final long OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS;
    public final long OFFSET_KERNEL_DATA_BASE_ROOTVNODE;

    /**
     * Constructor. The firmware version can be obtained
     * by making a call to <code>sceKernelGetProsperoSystemSwVersion</code>
     * method in <code>libkernel</code>. Last two bytes of the result return
     * the minor and the major version of the firmware.
     *
     * @param softwareVersion Firmware version in the form 0x[MAJOR BYTE][MINOR BYTE]
     */
    public KernelOffsets(int softwareVersion) {
        switch (softwareVersion) {
            case 0x0100:
            case 0x0101:
            case 0x0102:
            case 0x0105:
            case 0x0110:
            case 0x0111:
            case 0x0112:
            case 0x0113:
            case 0x0114:
            {
                OFFSET_KERNEL_DATA = 0x01B40000;

                // Note: at least on 1.02 the offset is confirmed to differ from Specter's.
                // Maybe not all ALLPROC addresses are valid?
                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x026D1BF8;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x06241074;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06565540;
                break;
            }
            case 0x0200:
            case 0x0220:
            case 0x0225:
            case 0x0226:
            case 0x0230:
            case 0x0250:
            case 0x0270:
            {
                OFFSET_KERNEL_DATA = 0x01B80000;

                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x02701C28;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x063E1274;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x067134C0;
                break;
            }
            case 0x0300:
            case 0x0320:
            {
                OFFSET_KERNEL_DATA = 0x0BD0000;

                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x0276DC58;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x06466474;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x067AB4C0;
                break;
            }
            case 0x0400:
            case 0x0402:
            case 0x0403:
            case 0x0450:
            case 0x0451:
            {
                OFFSET_KERNEL_DATA = 0x0C00000;

                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x027EDCB8;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x06506474;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x066E74C0;
                break;
            }
            case 0x0500:
            case 0x0502:
            case 0x0510:
            case 0x0550:
            {
                OFFSET_KERNEL_DATA = 0x0C50000;

                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x0290DD00;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x066366EC;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06843510;
                break;
            }
            default:
                String strSwVersion = MessageFormat.format(
                        "{0,number,#0}.{1,number,00}",
                        new Object[] {
                                new Integer((softwareVersion >> 8) & 0xFF),
                                new Integer(softwareVersion & 0xFF)
                        }
                );
                throw new SdkSoftwareVersionUnsupportedException(strSwVersion);
        }
    }
}

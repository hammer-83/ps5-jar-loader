package org.ps5jb.sdk.core.kernel;

import java.text.MessageFormat;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

/**
 * Class which is able to return various interesting offsets in kernel based on the console firmware version.
 * Note that currently not many firmware versions are supported
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
            case 0x0102:
            {
                OFFSET_KERNEL_DATA = 0x01B40000;

                OFFSET_KERNEL_DATA_BASE_ALLPROC = 0x026D1BF8;
                OFFSET_KERNEL_DATA_BASE_SECURITYFLAGS = 0x06241074;
                OFFSET_KERNEL_DATA_BASE_ROOTVNODE = 0x06565540;
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

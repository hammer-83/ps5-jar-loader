package org.ps5jb.client.payloads.umtx.common;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

public class KernelStackMarkerOffsets {
    public final long OFFSET_RET_FROM_MARKER_SELECT;
    public final long OFFSET_KBASE_FROM_RET_SELECT;
    public final long OFFSET_RET_FROM_MARKER_SCHED_YIELD;
    public final long OFFSET_KBASE_FROM_RET_SCHED_YIELD;

    /**
     * Constructor. The firmware version can be obtained
     * by making a call to <code>sceKernelGetProsperoSystemSwVersion</code>
     * method in <code>libkernel</code>. Last two bytes of the result return
     * the minor and the major version of the firmware.
     *
     * @param softwareVersion Firmware version in the form 0x[MAJOR BYTE][MINOR BYTE]
     */
    public KernelStackMarkerOffsets(int softwareVersion) {
        switch (softwareVersion) {
            case 0x0102:
            {
                OFFSET_RET_FROM_MARKER_SELECT = -0xC4;
                OFFSET_KBASE_FROM_RET_SELECT = -0x004D4108;
                OFFSET_RET_FROM_MARKER_SCHED_YIELD = -0xB0;
                OFFSET_KBASE_FROM_RET_SCHED_YIELD = -0x00563332;
                break;
            }
            default:
                throw new SdkSoftwareVersionUnsupportedException("Firmware not supported: 0x" + Integer.toHexString(softwareVersion));
        }
    }
}

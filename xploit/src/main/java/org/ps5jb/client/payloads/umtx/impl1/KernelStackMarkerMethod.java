package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;

public final class KernelStackMarkerMethod {
    public static final KernelStackMarkerMethod BLOCKING_SELECT = new KernelStackMarkerMethod(1, 16, 2, 100);

    // Buffer size for thread marker, it should not be larger than `SYS_IOCTL_SMALL_SIZE`,
    // otherwise `sys_ioctl` will use heap as storage instead of stack.
    // TODO: Not able to get IOCTL method to work...Maybe need to play with MAX_RECLAIM_SYSTEM_CALLS.
    public static final KernelStackMarkerMethod IOCTL = new KernelStackMarkerMethod(2, 128, 32, 250);

    public static final KernelStackMarkerMethod SCHED_YIELD = new KernelStackMarkerMethod(3, 0, 2, 100);

    private int id;
    private long markerSize;
    private int searchLoopInvocations;
    private long kernelStackWaitPeriod;

    private KernelStackMarkerMethod(int id, int markerSize, int searchLoopInvocations, long kernelStackWaitPeriod) {
        this.id = id;
        this.markerSize = markerSize;
        this.searchLoopInvocations = searchLoopInvocations;
        this.kernelStackWaitPeriod = kernelStackWaitPeriod;
    }

    public int getId() {
        return id;
    }

    public long getMarkerSize() {
        return markerSize;
    }

    public int getSearchLoopInvocations() {
        return searchLoopInvocations;
    }

    public long getKernelStackWaitPeriod() {
        return kernelStackWaitPeriod;
    }
}

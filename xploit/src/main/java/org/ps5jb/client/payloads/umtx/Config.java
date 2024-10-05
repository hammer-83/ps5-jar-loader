package org.ps5jb.client.payloads.umtx;

import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.sys.Pipe;
import org.ps5jb.sdk.include.sys.rtprio.RtPrio;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;

public class Config {
    // Configuration.
    public static final boolean dumpKernelStackPartially = true;
    public static final boolean dumpKernelStackOfReclaimThread = false;
    public static final boolean dumpKernelStackPointers = false;

    public static final boolean toggleSetThreadPriorities = false;
    public static final boolean toggleEnableThreadPriorityForReclaimThreads = false;
    public static final boolean toggleStoppingWorkingThreadsBeforeRemap = true;
    public static final boolean toggleReclaimCpuAffinityMask = true;
    public static final boolean toggleUnmappingOnFailure = true;

    public static final KernelStackMarkerMethod markerMethod = KernelStackMarkerMethod.SCHED_YIELD;

    // Common parameters.
    public static final int MAX_EXPLOITATION_ATTEMPTS = 100000;
    public static final int MAX_RACING_ITERATIONS = 50000;
    public static final int MAX_SHARED_MEMORY_KEYS = 3;
    public static int MAX_DUMMY_SHARED_MEMORY_OBJECTS = 0;
    public static final int MAX_DESTROYER_THREADS = 2;
    public static final int MAX_SPRAY_MUTEXES_PER_THREAD = 0x5;
    public static final int MAX_RECLAIM_THREADS = 100;
    public static final int MAX_RECLAIM_SYSTEM_CALLS = 1; // For `ioctl` method instead of `select`
    public static final int MAX_EXTRA_USER_MUTEXES = 1;
    public static final int MAX_DESCRIPTORS = 0x3FF;

    // Amounts of milliseconds we need to wait at different steps.
    public static final long INITIAL_WAIT_PERIOD = 50; // 50
    public static final long TINY_WAIT_PERIOD = 50; // 50

    // Special marker to determine victim thread's ID.
    public static final int RECLAIM_THREAD_MARKER_BASE = 0x00414141;

    public static final long MAX_PIPE_BUFFER_SIZE = Pipe.PIPE_MINDIRECT / 2;

    // Priorities for such threads. `RTP_PRIO_FIFO` should also work.
    public static RtPrio MAIN_THREAD_PRIORITY = new RtPrio(RtPrioType.RTP_PRIO_REALTIME, (short) 256);
    public static RtPrio DESTROYER_THREAD_PRIORITY = new RtPrio(RtPrioType.RTP_PRIO_REALTIME, (short) 256); // 256
    public static RtPrio LOOKUP_THREAD_PRIORITY = new RtPrio(RtPrioType.RTP_PRIO_REALTIME, (short) 767); // 767, 400
    public static RtPrio RECLAIM_THREAD_PRIORITY = new RtPrio(RtPrioType.RTP_PRIO_REALTIME, (short) 450); // 450

    // Number of times kernel thread's heap pointer should occur in kernel stack to
    // distinguish it from other values on stack.
    public static int KERNEL_THREAD_POINTER_OCCURRENCE_THRESHOLD = 10;

    // Max length of reclaim thread name.
    public static int MAX_RECLAIM_THREAD_NAME_SIZE = 0x10;
}

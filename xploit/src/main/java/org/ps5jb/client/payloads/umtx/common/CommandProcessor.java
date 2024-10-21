package org.ps5jb.client.payloads.umtx.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.Pipe;
import org.ps5jb.sdk.lib.LibKernel;

public class CommandProcessor {
    private LibKernel libKernel;

    public static final int KPRIM_NOP = 0;
    public static final int KPRIM_READ = 1;
    public static final int KPRIM_WRITE = 2;
    public static final int KPRIM_EXIT = 3;

    public AtomicBoolean exitSignal = new AtomicBoolean();
    public AtomicInteger cmd = new AtomicInteger();
    public AtomicLong uaddr = new AtomicLong();
    public AtomicLong kaddr = new AtomicLong();
    public AtomicInteger len = new AtomicInteger();
    public AtomicLong readCounter = new AtomicLong();
    public AtomicLong writeCounter = new AtomicLong();

    public int pipeReadFd = -1;
    public int pipeWriteFd = -1;
    public Pointer pipeScratchBuf;
    public Pointer readValue;
    public Pointer writeValue;

    public CommandProcessor() {
        libKernel = new LibKernel();

        Pointer pipeFds = Pointer.calloc(8);
        try {
            int returnCode = libKernel.pipe(pipeFds);
            if (returnCode < 0) {
                DebugStatus.error("[-] Failed to create pipe, errno: " + libKernel.__error().read4());
                throw new RuntimeException("Initialization failed");
            }
            this.pipeReadFd = pipeFds.read4();
            this.pipeWriteFd = pipeFds.read4(4);
            this.pipeScratchBuf = Pointer.calloc(Pipe.BIG_PIPE_SIZE);

            this.readValue = Pointer.calloc(0x8L);
            this.writeValue = Pointer.calloc(0x8L);
        } finally {
            pipeFds.free();
        }
    }

    public void free() {
        if (pipeScratchBuf != null) {
            pipeScratchBuf.free();;
            pipeScratchBuf = null;
        }

        if (pipeReadFd != -1) {
            libKernel.close(pipeReadFd);
        }

        if (pipeWriteFd != -1) {
            libKernel.close(pipeWriteFd);
        }

        libKernel.closeLibrary();
    }

    public void handleCommands() {
        while (!exitSignal.get()) {
            int cmd = this.cmd.get();
            if (cmd == KPRIM_NOP) {
                Thread.yield();
                continue;
            }
            long len = this.len.get();

            switch (cmd) {
                case KPRIM_READ:
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("[+] Command processor: blocking to write " + len + " bytes for READ command");
                    }
                    this.readCounter.incrementAndGet();
                    long read = this.libKernel.write(this.pipeWriteFd, this.pipeScratchBuf, len);
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("[+] Command processor: written " + read + " bytes");
                    }
                    break;

                case KPRIM_WRITE:
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("[+] Command processor: blocking to read " + len + " bytes for WRITE command");
                    }
                    this.writeCounter.incrementAndGet();
                    long write = this.libKernel.read(this.pipeReadFd, this.pipeScratchBuf, len);
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("[+] Command processor: read " + write + " bytes");
                    }
                    break;

                case KPRIM_EXIT:
                    // Do nothing
                    DebugStatus.error("Command processor: exiting");
                    exitSignal.set(true);
                    break;

                default:
                    DebugStatus.error("Command processor: unknown command");
            }

            this.cmd.set(KPRIM_NOP);

            DebugStatus.notice("Command processor: resetting");
        }

        free();

        DebugStatus.info("Command processor: finished and cannot be reused");
    }
}

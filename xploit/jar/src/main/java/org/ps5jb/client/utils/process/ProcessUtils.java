package org.ps5jb.client.utils.process;

import org.ps5jb.client.PayloadConstants;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.RtPrio;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.rtprio.RtPrioType;
import org.ps5jb.sdk.include.sys.rtprio.SchedulingClass;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Utility class for various operations on kernel processes.
 */
public class ProcessUtils {
    private final KernelPointer kbaseAddress;
    private final KernelOffsets offsets;
    private final LibKernel libKernel;

    /**
     * Constructor taking only libKernel instance as the argument.
     * The instance of this class should only be used by the same thread
     * as the thread that is using the supplied LibKernel instance.
     *
     * @param libKernel Instance of LibKernel native library.
     */
    public ProcessUtils(LibKernel libKernel) {
        KernelAccessor kernelAccessor = KernelReadWrite.getAccessor(getClass().getClassLoader());
        if (kernelAccessor == null) {
            throw new SdkRuntimeException("Kernel R/W is required to instantiate this class.");
        }

        this.libKernel = libKernel;
        this.kbaseAddress = KernelPointer.valueOf(kernelAccessor.getKernelBase());

        int sw = libKernel.getSystemSoftwareVersion();
        this.offsets = new KernelOffsets(sw);
    }

    /**
     * Constructor taking existing instances of the required utility classes.
     * The instance of this class should only be used by the same thread
     * as the thread that is using the supplied LibKernel instance.
     *
     * @param libKernel An instance of LibKernel library.
     * @param kbaseAddress Base address of the kernel.
     * @param offsets Kernel offsets calculated for the current firmware.
     */
    public ProcessUtils(LibKernel libKernel, KernelPointer kbaseAddress, KernelOffsets offsets) {
        this.libKernel = libKernel;
        this.offsets = offsets;
        this.kbaseAddress = kbaseAddress;
    }

    /**
     * Get <code>proc</code> structure of the current process.
     *
     * @return Current process' <code>proc</code> structure.
     */
    public org.ps5jb.sdk.include.sys.proc.Process getCurrentProcess() {
        int curPid = libKernel.getpid();

        KernelPointer allproc = KernelPointer.NULL;
        if (KernelPointer.NULL.equals(kbaseAddress) || kbaseAddress == null) {
            String allProcAddrStr = System.getProperty(PayloadConstants.ALLPROC_ADDRESS_PROPERTY);
            if (allProcAddrStr != null) {
                allproc = KernelPointer.valueOf(Long.parseLong(allProcAddrStr));
            }
        } else {
            KernelPointer kdataAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA);
            allproc = kdataAddress.inc(offsets.OFFSET_KERNEL_DATA_BASE_ALLPROC);
        }

        KernelPointer.nonNull(allproc, "Kernel allproc address could not be determined");

        Process curProc = new Process(KernelPointer.valueOf(allproc.read8()));
        while (curProc != null) {
            int pid = libKernel.getpid();
            if (pid == curPid) {
                break;
            }
            curProc = curProc.getNextProcess();
        }

        return curProc;
    }

    /**
     * Returns the ids for the user and group of a given process.
     *
     * @param proc Process whose user/group to set.
     * @return Array of ids for user/group values in that order: uid, ruid, svuid, ngroups and rgid.
     */
    public int[] getUserGroup(Process proc) {
        KernelPointer ucredAddr = proc.getUCred();

        int[] result = new int[5];
        result[0] = ucredAddr.read4(0x04);   // cr_uid
        result[1] = ucredAddr.read4(0x08);   // cr_ruid
        result[2] = ucredAddr.read4(0x0C);   // cr_svuid
        result[3] = ucredAddr.read4(0x10);   // cr_ngroups
        result[4] = ucredAddr.read4(0x14);   // cr_rgid

        return result;
    }

    /**
     * Set the ids for the user and group of a given process.
     *
     * @param proc Process whose user/group to set.
     * @param ids Array of ids to set in that order: uid, ruid, svuid, ngroups and rgid.
     * @return Value of ids before the modification.
     */
    public int[] setUserGroup(Process proc, int[] ids) {
        int[] prevValue = getUserGroup(proc);

        KernelPointer ucredAddr = proc.getUCred();
        ucredAddr.write4(0x04, ids[0]);   // cr_uid
        ucredAddr.write4(0x08, ids[1]);   // cr_ruid
        ucredAddr.write4(0x0C, ids[2]);   // cr_svuid
        ucredAddr.write4(0x10, ids[3]);   // cr_ngroups
        ucredAddr.write4(0x14, ids[4]);   // cr_rgid

        return prevValue;
    }

    /**
     * Gets PS5-specific privileges for a given process.
     *
     * @param proc Process whose privileges to get.
     * @return Array of privileges in that order: authId, caps0, caps1, attrs.
     */
    public long[] getPrivs(Process proc) {
        KernelPointer ucredAddr = proc.getUCred();

        long[] result = new long[4];
        result[0] = ucredAddr.read8(0x58);         // cr_sceAuthId
        result[1] = ucredAddr.read8(0x60);         // cr_sceCaps[0]
        result[2] = ucredAddr.read8(0x68);         // cr_sceCaps[1]
        result[3] = ucredAddr.read1(0x83);         // cr_sceAttr[0]

        return result;
    }

    /**
     * Sets PS5-specific privileges for a given process.
     *
     * @param proc Process whose privileges to set.
     * @param privs Array of privileges in that order: authId, caps0, caps1, attrs.
     * @return Value of privs before the modification.
     */
    public long[] setPrivs(Process proc, long[] privs) {
        long[] prevValue = getPrivs(proc);

        KernelPointer ucredAddr = proc.getUCred();
        ucredAddr.write8(0x58, privs[0]);         // cr_sceAuthId
        ucredAddr.write8(0x60, privs[1]);         // cr_sceCaps[0]
        ucredAddr.write8(0x68, privs[2]);         // cr_sceCaps[1]
        ucredAddr.write1(0x83, (byte) privs[3]);  // cr_sceAttr[0]

        return prevValue;
    }

    /**
     * This method can be used to set the priority of Java thread and of the
     * corresponding native thread.
     *
     * @param javaPriority Priority to set for the java thread. See {@link Thread#setPriority(int)}.
     *   If null, Java thread priority will not be touched.
     * @param rtPrioValue Priority of the native thread. See {@link RtPrio#setRtPrio(int, RtPrioType)}.
     * @throws SdkException If native priority value is not null and native call failed.
     */
    public void setCurrentThreadPriority(Integer javaPriority, Short rtPrioValue) throws SdkException {
        if (javaPriority != null) {
            Thread.currentThread().setPriority(javaPriority.intValue());
        }

        if (rtPrioValue != null) {
            RtPrio rtPrio = new RtPrio(libKernel);
            rtPrio.setRtPrio(0, new RtPrioType(SchedulingClass.RTP_PRIO_REALTIME, rtPrioValue.shortValue()));
        }
    }
}

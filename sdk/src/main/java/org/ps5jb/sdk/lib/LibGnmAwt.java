package org.ps5jb.sdk.lib;

import java.util.HashSet;
import java.util.Set;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Library;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.PMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapEntryMask;
import org.ps5jb.sdk.include.sys.proc.Process;

public class LibGnmAwt extends Library {
    private Pointer lockFunc;
    private Pointer dmaFunc;
    private Pointer unlockFunc;
    private Pointer syncObject;
    private Pointer syncValuePtr;

    /**
     * Constructor for LibAwtGnm. Note that before calling the internal functions in this
     * library, it's necessary to resolve them by calling
     * {@link #resolveInternal(KernelPointer)}.
     */
    public LibGnmAwt() {
        // Use path because the module handle differs from firmware to firmware.
        super("libgnmawt.prx");
    }

    /**
     * Same as {@link #resolveInternal(Process)} but the current process structure is
     * created from the supplied pointer.
     *
     * @param curProcAddr Pointer to <code>proc</code> structure of the current process.
     */
    public void resolveInternal(KernelPointer curProcAddr) {
        org.ps5jb.sdk.include.sys.proc.Process curProc = new Process(curProcAddr);
        resolveInternal(curProc);
    }

    /**
     * Resolves addresses of internal objects. This method modifies page tables
     * so dynamic kernel memory parameters are assumed to have been
     * {@link PMap#refresh(KernelPointer, KernelPointer, KernelPointer) refreshed}.
     *
     * @param curProc Current process structure.
     */
    public void resolveInternal(Process curProc) {
        PhysicalMap userPmap = curProc.getVmSpace().getPhysicalMap();

        Set processedPtes = new HashSet();

        // Obtain the pointer to "Java_sun_awt_GnmUtils_copyPlanesBackgroundToPrimary" function
        final Pointer refFuncAddr = addrOf("Java_sun_awt_GnmUtils_copyPlanesBackgroundToPrimary");
        final long refFuncSize = 0x110;

        // Patch page tables to remove XO flag from reference function page(s)
        removeXo(userPmap, processedPtes, refFuncAddr, refFuncSize);

        // Read offsets to target functions from the reference function code
        Pointer callToLock = refFuncAddr.inc(0x0E);
        lockFunc = Pointer.valueOf(callToLock.addr() + 4 + callToLock.read4());

        Pointer callToDma = refFuncAddr.inc(0x74);
        dmaFunc = Pointer.valueOf(callToDma.addr() + 4 + callToDma.read4());

        Pointer jmpToUnlock = refFuncAddr.inc(0xFF);
        unlockFunc = Pointer.valueOf(jmpToUnlock.addr() + 4 + jmpToUnlock.read4());

        // DMA function also has a synchronization object that changes value when GPU processed the operation.
        // Can be used to check for DMA completion.
        final Pointer refSyncObjectInstrAddr = dmaFunc.inc(0x112);
        final long refSyncInstrSize = 0x28;
        removeXo(userPmap, processedPtes, refSyncObjectInstrAddr, refSyncInstrSize);

        // Read offset to the sync object location in library data section
        final Pointer syncObjectPtr = Pointer.valueOf(refSyncObjectInstrAddr.addr() + 4 + refSyncObjectInstrAddr.read4());
        syncObject = Pointer.valueOf(syncObjectPtr.read8());

        // Read offset to the sync value pointer in library data section
        final Pointer refSyncValueInstrAddr = refSyncObjectInstrAddr.inc(0x24);
        syncValuePtr = Pointer.valueOf(refSyncValueInstrAddr.addr() + 4 + refSyncValueInstrAddr.read4());
    }

    /**
     * Internal helper method to remove execute-only bit on library code pages.
     * This is needed to determine some library object offsets which are not exported.
     *
     * @param userPmap User address space physical map.
     * @param processedPtes Set of PTEs for tracking pages that were already processed.
     * @param userAddress User address where the execute-only bit should be removed.
     * @param size Size of data that is affected by removal of execute-only bit.
     */
    private void removeXo(PhysicalMap userPmap, Set processedPtes, Pointer userAddress, long size) {
        KernelPointer[] pteAddresses = new KernelPointer[] {
                PMap.pmap_pte(userPmap, userAddress.addr()),
                PMap.pmap_pte(userPmap, userAddress.inc(size).addr())
        };

        for (KernelPointer pteAddr : pteAddresses) {
            if (!processedPtes.contains(pteAddr)) {
                long pte = pteAddr.read8();
                long newPte = pte & ~PhysicalMapEntryMask.SCE_PG_XO.value();
                newPte = newPte | PhysicalMapEntryMask.X86_PG_RW.value();
                pteAddr.write8(newPte);
                processedPtes.add(pteAddr);
            }
        }
    }

    /**
     * Lock to begin DMA.
     */
    public void internalLock() {
        call(lockFunc);
    }

    /**
     * Unlock to end DMA.
     */
    public void internalUnlock() {
        call(unlockFunc);
    }

    /**
     * Perform DMA.
     *
     * @param destAddress Address to write to.
     * @param srcAddress Address to read from.
     * @param size Size of transferred data.
     */
    public void internalDirectMemoryCopy(Pointer destAddress, Pointer srcAddress, int size) {
        call(dmaFunc, destAddress.addr(), srcAddress.addr(), size);
    }

    /**
     * Synchronization object that contains a counter value.
     *
     *
     * @return Pointer to the DMA sync object.
     */
    public Pointer getSyncObject() {
        return syncObject;
    }

    /**
     * Current counter of the synchronization object.
     * It should be read before invoking
     * {@link #internalDirectMemoryCopy(Pointer, Pointer, int)}.
     * After the DMA is complete, GPU will increment this value
     * and place it in {@link #getSyncObject()}.
     * This will be an indicator that transfer is completed.
     *
     * @return Current sync counter value.
     */
    public int getSyncValue() {
        return syncValuePtr.read4();
    }

    /**
     * Wait for sync value after the DMA request.
     *
     * @param syncValueGoal Value that the counter in {@link #getSyncObject()}
     *   should reach.
     * @param timeoutMillis Number of milliseconds to wait.
     * @return True if the counter reached the desired value. False if the
     *   timeout occurred and the counter did not reach the goal.
     */
    public boolean waitSync(int syncValueGoal, long timeoutMillis) {
        boolean goalReached;

        int newValue;
        long start = 0;
        do {
            if (start != 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Ignore
                }
            } else {
                start = System.currentTimeMillis();
            }

            newValue = syncObject.read4();
            goalReached = newValue >= syncValueGoal;
        } while (!goalReached && ((System.currentTimeMillis() - start) < timeoutMillis));

        return goalReached;
    }
}

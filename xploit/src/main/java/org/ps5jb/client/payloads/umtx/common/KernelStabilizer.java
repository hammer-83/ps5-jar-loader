package org.ps5jb.client.payloads.umtx.common;

import java.util.Collection;
import java.util.Iterator;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Attempt to patch various references in the kernel to avoid the panic.
 * This process is better performed once permanent kernel accessor is
 * installed.
 */
public class KernelStabilizer {
    private static final long OFFSET_SHMFD_SHM_REFS = 16L;
    private static final long OFFSET_FILE_F_DATA = 0L;
    private static final long OFFSET_FILE_F_TYPE = 32L;
    private static final long OFFSET_FILE_F_COUNT = 40L;
    private static final long OFFSET_THREAD_KSTACK_OBJ = 1128L;
    private static final long OFFSET_THREAD_KSTACK = 1136L;
    private static final long OFFSET_PROC_P_VMSPACE = 512L;
    private static final long OFFSET_VMSPACE_VM_MAP = 0L;
    private static final long OFFSET_VM_MAP_ENTRY_START = 32L;
    private static final long OFFSET_VM_MAP_ENTRY_OBJECT = 80L;
    private static final long OFFSET_VM_MAP_ENTRY_NEXT = 8L;
    private static final long OFFSET_VM_OBJECT_REF_COUNT = 132L;

    private final LibKernel libKernel;

    /**
     * Default constructor
     */
    public KernelStabilizer() {
        libKernel = new LibKernel();
    }

    public void free() {
        libKernel.closeLibrary();
    }

    /**
     * Wipes kstack from the native thread structure and stabilizes the asociated vm object ref count.
     *
     * @param threadAddress Address of the thread struct of the reclaimed thread.
     */
    public void fixupKernelStack(KernelPointer threadAddress) {
        KernelPointer kstack_obj_ptr = KernelPointer.valueOf(threadAddress.read8(OFFSET_THREAD_KSTACK_OBJ));    // struct thread -> struct vm_object *td_kstack_obj

        // Wipe `td_kstack`, thus kernel would not try to destroy it.
        threadAddress.write8(OFFSET_THREAD_KSTACK, 0L);                                                   // struct thread -> vm_offset_t td_kstack
        kstack_obj_ptr.write4(OFFSET_VM_OBJECT_REF_COUNT, 0x10);                                          // struct vm_object -> int ref_count
    }

    /**
     * For mapped memory areas which did not successfully receive kstack and were not upmapped,
     * increase the reference count so that kernel does not attempt to free them.
     *
     * @param processAddress Address of the proc structure.
     * @param mappedKernelStackAddresses Collection of mapped addresses to process. Each element
     *   is expected to be of type Pointer.
     * @return Number of fixes applied to the kernel vm space.
     */
    public int fixupVmSpace(KernelPointer processAddress, Collection mappedKernelStackAddresses) {
        int numFixes = 0;
        if (mappedKernelStackAddresses != null) {
            final int stackUserAddressCount = mappedKernelStackAddresses.size();

            KernelPointer vmSpaceAddress = KernelPointer.valueOf(processAddress.read8(OFFSET_PROC_P_VMSPACE));
            KernelPointer vmMapAddress = KernelPointer.valueOf(vmSpaceAddress.read8(OFFSET_VMSPACE_VM_MAP));

            while (!KernelPointer.NULL.equals(vmMapAddress) && (numFixes < stackUserAddressCount)) {
                if (fixVmMapEntry(vmMapAddress, mappedKernelStackAddresses)) {
                    numFixes++;
                }
                vmMapAddress = KernelPointer.valueOf(vmMapAddress.read8(OFFSET_VM_MAP_ENTRY_NEXT));
            }
        }
        return numFixes;
    }

    /**
     * Fixes various references inside open files array. Closes lookup descriptor after fixes.
     *
     * @param openFilesAddress Kernel pointer to {@link KernelOffsetsCalculator#processOpenFilesAddress}.
     * @param lookupDescriptor Lookup descriptor that got reclaimed in UMTX exploit execution.
     * @return Result of calling close on the lookupDescriptor.
     * @throws SdkException If error occurs.
     * @throws IllegalAccessError If openFilesAddress is invalid.
     */
    public int fixupSharedMemory(KernelPointer openFilesAddress, int lookupDescriptor) throws SdkException {
        KernelPointer.validRange(openFilesAddress);

        if (lookupDescriptor == -1) {
            throw new NotFoundException("Lookup descriptor of primary shared memory object not found");
        }

        KernelPointer fileDescEntryAddress = openFilesAddress.inc(lookupDescriptor * 0x30L);           // fdt_ofiles[lookup_fd], sizeof(filedescent) = 0x30
        KernelPointer fileAddress = KernelPointer.valueOf(fileDescEntryAddress.read8());                     // struct filedescent -> struct file *fde_file

        final KernelPointer sharedMemoryFileDescAddress = KernelPointer.valueOf(fileAddress.read8(OFFSET_FILE_F_DATA));       // struct file -> void* f_data (struct shmfd*)
        if (!KernelPointer.NULL.equals(sharedMemoryFileDescAddress)) {
            KernelPointer shmRefCountAddress = sharedMemoryFileDescAddress.inc(OFFSET_SHMFD_SHM_REFS);                        // struct shmfd -> int shm_refs
            shmRefCountAddress.write4(0x10);
        }

        KernelPointer fCountAddress = fileAddress.inc(OFFSET_FILE_F_COUNT);                                                   // struct file -> volatile u_int f_count
        fCountAddress.write4(0x10);

        return libKernel.close(lookupDescriptor);
    }

    private boolean fixVmMapEntry(KernelPointer mapEntryKernelAddress, Collection mappedKernelStackAddresses) {
        boolean matched = false;

        final long startUserAddress = mapEntryKernelAddress.read8(OFFSET_VM_MAP_ENTRY_START);

        final Iterator iterator = mappedKernelStackAddresses.iterator();
        while (iterator.hasNext()) {
            final Pointer userAddress = (Pointer) iterator.next();
            if (userAddress.addr() == startUserAddress) {
                final KernelPointer objectAddress = KernelPointer.valueOf(mapEntryKernelAddress.read8(OFFSET_VM_MAP_ENTRY_OBJECT));
                if (!KernelPointer.NULL.equals(objectAddress)) {
                    final KernelPointer refCountAddress = objectAddress.inc(OFFSET_VM_OBJECT_REF_COUNT);
                    refCountAddress.write4(0x10);
                }
                matched = true;
                break;
            }
        }

        return matched;
    }

    /**
     * Fixup references to possibly corrupt descriptors
     *
     * @param openFilesAddress Address of open file descriptors array in the current process.
     * @param usedDescriptors Collections of descriptors to fix
     * @return Number of modified descriptors
     */
    public int fixUsedDescriptors(KernelPointer openFilesAddress, Collection usedDescriptors) {
        int numFixes = 0;

        final Iterator iterator = usedDescriptors.iterator();
        while (iterator.hasNext()) {
            final int descriptor = ((Integer) iterator.next()).intValue();

            KernelPointer fileDescEntryAddress = openFilesAddress.inc(descriptor * 0x30L);
            KernelPointer fileAddress = KernelPointer.NULL;
            if (!KernelPointer.NULL.equals(fileDescEntryAddress)) {
                fileAddress = KernelPointer.valueOf(fileDescEntryAddress.read8());
            }

            if (!KernelPointer.NULL.equals(fileAddress)) {
                final short fileType = fileAddress.read2(OFFSET_FILE_F_TYPE);        // struct file -> short f_type
                if (fileType == 8) { // DTYPE_SHM
                    // Reset file pointer of exploited shared memory file object. This is workaround for `shm_drop` crash after `shmfd`
                    // being reused, so `shm_object` may contain garbage pointer, and it can be dereferenced there.

                    // TODO: Check if needed (causes crashes sometimes?):
                    //fileDescEntryAddress.write8(0x00, 0L);                         // struct filedescent -> fde_file
                    numFixes++;
                }
            }
        }

        return numFixes;
    }
}

package org.ps5jb.sdk.include.vm.map;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMap;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapFlag;
import org.ps5jb.sdk.include.machine.pmap.PhysicalMapType;
import org.ps5jb.sdk.include.sys.mutex.MutexType;

/**
 * Incomplete wrapper for FreeBSD <code>vmspace</code> structure.
 */
public class VmSpace {
    public static final long OFFSET_VM_MAP = 0;
    public static final long OFFSET_VM_PMAP = 704L;

    private final KernelPointer ptr;

    /**
     * VmSpace constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing VmSpace data.
     */
    public VmSpace(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * VM address map.
     *
     * @return Returns the value of <code>vm_map</code> field of <code>vmspace</code> structure.
     */
    public KernelPointer getMap() {
        return KernelPointer.valueOf(this.ptr.read8(OFFSET_VM_MAP));
    }

    /**
     * Regular or nested tables.
     *
     * @return Returns the value of <code>vm_pmap</code> field of <code>pmap</code> structure.
     */
    public PhysicalMap getPhysicalMap() {
        return new PhysicalMap(getPointer().inc(OFFSET_VM_PMAP));
    }

    /**
     * Gets the native memory pointer where this VmSpace's data is stored.
     *
     * @return VmSpace memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}

package org.ps5jb.sdk.include.sys.proc;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.Param;
import org.ps5jb.sdk.include.sys.mutex.MutexType;
import org.ps5jb.sdk.include.vm.map.VmSpace;

/**
 * Incomplete wrapper for FreeBSD <code>proc</code> structure.
 */
public class Process {
    public static final long OFFSET_P_LIST_LE_NEXT = 0L;
    public static final long OFFSET_P_LIST_LE_PREV = OFFSET_P_LIST_LE_NEXT + 8L;
    public static final long OFFSET_P_THREADS_TQH_FIRST = OFFSET_P_LIST_LE_PREV + 8L;
    public static final long OFFSET_P_THREADS_TQH_LAST = OFFSET_P_THREADS_TQH_FIRST + 8L;
    public static final long OFFSET_P_SLOCK = OFFSET_P_THREADS_TQH_LAST + 8L;
    public static final long OFFSET_P_UCRED = OFFSET_P_SLOCK + MutexType.SIZE;
    public static final long OFFSET_P_FD = OFFSET_P_UCRED + 8L;
    public static final long OFFSET_P_PID = 188L;
    public static final long OFFSET_P_PPTR = 232L;
    public static final long OFFSET_P_VM_SPACE = 512L;
    public static final long OFFSET_P_DYNLIB = 1000L;
    public static final long OFFSET_P_COMM = 1080L;

    private final KernelPointer ptr;
    private MutexType slock;
    private VmSpace vmSpace;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing Process data.
     */
    public Process(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Address of previous next element.
     *
     * @return Returns the value of <code>p_list.le_prev</code> field of <code>proc</code> structure.
     */
    public KernelPointer getPreviousProcessPointer() {
        return new KernelPointer(ptr.read8(OFFSET_P_LIST_LE_PREV), null, this.ptr.getKernelAccessor());
    }

    /**
     * Next element.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_list.le_next</code> field of <code>proc</code> structure.
     *   If <code>le_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getNextProcess() {
        KernelPointer next = new KernelPointer(ptr.read8(OFFSET_P_LIST_LE_NEXT), null, this.ptr.getKernelAccessor());
        if (KernelPointer.NULL.equals(next)) {
            return null;
        }
        return new Process(next);
    }

    /**
     * Parent process.
     *
     * @return Returns the Process pointed to by the value of
     *   <code>p_pptr</code> field of <code>proc</code> structure.
     *   If <code>p_pptr</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Process getParentProcess() {
        KernelPointer pptr = new KernelPointer(ptr.read8(OFFSET_P_PPTR), null, this.ptr.getKernelAccessor());
        if (KernelPointer.NULL.equals(pptr)) {
            return null;
        }
        return new Process(pptr);
    }

    /**
     * Process spin lock.
     *
     * @return Returns the value of <code>p_slock</code> field of <code>proc</code> structure.
     */
    public MutexType getSpinLock() {
        if (slock == null) {
            slock = new MutexType(new KernelPointer(ptr.read8(OFFSET_P_SLOCK), new Long(MutexType.SIZE), this.ptr.getKernelAccessor()));
        }
        return slock;
    }

    /**
     * Process owner's identity.
     *
     * @return Returns the value of <code>p_ucred</code> field of <code>proc</code> structure.
     */
    public KernelPointer getUCred() {
        return new KernelPointer(ptr.read8(OFFSET_P_UCRED), null, this.ptr.getKernelAccessor());
    }

    /**
     * Open files.
     *
     * @return Returns the value of <code>p_fd</code> field of <code>proc</code> structure.
     */
    public KernelPointer getFd() {
        return new KernelPointer(ptr.read8(OFFSET_P_FD), null, this.ptr.getKernelAccessor());
    }

    /**
     * Process dynamic library info (PS5-specific structure).
     *
     * @return Returns the pointer to dynamic library info structure of the process.
     */
    public KernelPointer getDynLib() {
        return new KernelPointer(ptr.read8(OFFSET_P_DYNLIB), null, this.ptr.getKernelAccessor());
    }

    /**
     * Process identifier.
     *
     * @return Returns the value of <code>p_pid</code> field of <code>proc</code> structure.
     */
    public int getPid() {
        return ptr.read4(OFFSET_P_PID);
    }

    /**
     * Address space.
     *
     * @return Returns the value of <code>p_vmspace</code> field of <code>proc</code> structure.
     */
    public VmSpace getVmSpace() {
        if (vmSpace == null) {
            vmSpace = new VmSpace(new KernelPointer(ptr.read8(OFFSET_P_VM_SPACE), null, this.ptr.getKernelAccessor()));
        }
        return vmSpace;
    }

    /**
     * Process name.
     *
     * @return Returns the value of <code>p_comm</code> field of <code>proc</code> structure.
     */
    public String getName() {
        return ptr.readString(OFFSET_P_COMM, new Integer(Param.MAXCOMLEN), Charset.defaultCharset().name());
    }

    /**
     * Gets the native memory pointer where this Process's data is stored.
     *
     * @return Process memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}

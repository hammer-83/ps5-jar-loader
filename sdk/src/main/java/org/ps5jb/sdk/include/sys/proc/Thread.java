package org.ps5jb.sdk.include.sys.proc;

import java.nio.charset.Charset;

import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.Param;
import org.ps5jb.sdk.include.sys.mutex.MutexType;

/**
 * Incomplete wrapper for FreeBSD <code>thread</code> structure.
 */
public class Thread {
    public static final long OFFSET_TD_MTX = 0L;
    public static final long OFFSET_TD_PROC = OFFSET_TD_MTX + 8L;
    public static final long OFFSET_TD_PLIST_TQE_NEXT = OFFSET_TD_PROC + 8L;
    public static final long OFFSET_TD_PLIST_TQE_PREV = OFFSET_TD_PLIST_TQE_NEXT + 8L;
    public static final long OFFSET_TD_TID = 156L;
    public static final long OFFSET_TD_NAME = 660L;

    private final KernelPointer ptr;
    private MutexType mtx;

    /**
     * Process constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing Thread data.
     */
    public Thread(KernelPointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Next thread in process.
     *
     * @return Returns the Thread pointed to by the value of
     *   <code>td_plist.tqe_next</code> field of <code>thread</code> structure.
     *   If <code>tqe_next</code> is <code>NULL</code>, the return value
     *   of this method is <code>null</code>.
     */
    public Thread getNextThread() {
        KernelPointer thNext = this.ptr.pptr(OFFSET_TD_PLIST_TQE_NEXT);
        if (KernelPointer.NULL.equals(thNext)) {
            return null;
        }
        return new Thread(thNext);
    }

    /**
     * Thread mutex (replaces sched lock).
     *
     * @return Returns the value of <code>td_mtx</code> field of <code>thread</code> structure.
     */
    public MutexType getMtx() {
        if (mtx == null) {
            mtx = new MutexType(this.ptr.pptr(OFFSET_TD_MTX, new Long(MutexType.SIZE)));
        }
        return mtx;
    }

    /**
     * Thread identifier.
     *
     * @return Returns the value of <code>p_tid</code> field of <code>thread</code> structure.
     */
    public int getTid() {
        return ptr.read4(OFFSET_TD_TID);
    }

    /**
     * Thread name.
     *
     * @return Returns the value of <code>td_name</code> field of <code>thread</code> structure.
     */
    public String getName() {
        return ptr.readString(OFFSET_TD_NAME, new Integer(Param.MAXCOMLEN + 1), Charset.defaultCharset().name());
    }

    /**
     * Gets the native memory pointer where this Thread's data is stored.
     *
     * @return Thread memory pointer.
     */
    public KernelPointer getPointer() {
        return this.ptr;
    }
}

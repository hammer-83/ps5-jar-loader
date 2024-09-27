package org.ps5jb.sdk.include.sys.uio;

import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper for FreeBSD Kernel <code>uio</code> structure.
 */
public class UioType {
    public static final long SIZE = 48L;
    public static final long OFFSET_IOV = 0L;
    public static final long OFFSET_IOV_COUNT = 8L;
    public static final long OFFSET_OFFSET = 16L;
    public static final long OFFSET_RESIDUAL_SIZE = 24L;
    public static final long OFFSET_SEGMENT_FLAG = 32L;
    public static final long OFFSET_READ_WRITE = 36L;
    public static final long OFFSET_OWNER = 40L;

    private final Pointer ptr;
    private final boolean ownPtr;

    /**
     * UioType default constructor.
     */
    public UioType() {
        this.ptr = Pointer.calloc(SIZE);
        this.ownPtr = true;
    }

    /**
     * UioType constructor from existing pointer.
     *
     * @param ptr Existing pointer to native memory containing UioType data.
     */
    public UioType(Pointer ptr) {
        this.ptr = ptr;
        this.ownPtr = false;
    }

    /**
     * Scatter/gather list.
     *
     * @return Returns the value of <code>uio_iov</code> field of <code>uio</code> structure,
     *   which is a pointer to a list of {@link org.ps5jb.sdk.include.sys.iovec.IoVecType} structures.
     */
    public Pointer getIov() {
        return Pointer.valueOf(this.ptr.read8(OFFSET_IOV));
    }

    /**
     * Length of scatter/gather list.
     *
     * @return Returns the value of <code>uio_iovcnt</code> field of <code>uio</code> structure.
     */
    public int getIovCount() {
        return this.ptr.read4(OFFSET_IOV_COUNT);
    }

    /**
     * Offset in target object.
     *
     * @return Returns the value of <code>uio_offset</code> field of <code>uio</code> structure.
     */
    public long getOffset() {
        return this.ptr.read8(OFFSET_OFFSET);
    }

    /**
     * Remaining bytes to process.
     *
     * @return Returns the value of <code>uio_resid</code> field of <code>uio</code> structure.
     */
    public long getResidualSize() {
        return this.ptr.read8(OFFSET_RESIDUAL_SIZE);
    }

    /**
     * Address space.
     *
     * @return Returns the value of <code>uio_segflg</code> field of <code>uio</code> structure.
     */
    public UioSegmentFlag getSegmentFlag() {
        return UioSegmentFlag.valueOf(this.ptr.read4(OFFSET_SEGMENT_FLAG));
    }

    /**
     * Set address space.
     *
     * @param val New length.
     */
    public void setSegmentFlag(UioSegmentFlag val) {
        this.ptr.write4(OFFSET_SEGMENT_FLAG, val.value());
    }

    /**
     * Operation.
     *
     * @return Returns the value of <code>uio_rw</code> field of <code>uio</code> structure.
     */
    public UioReadWrite getReadWrite() {
        return UioReadWrite.valueOf(this.ptr.read4(OFFSET_READ_WRITE));
    }

    /**
     * Owner thread.
     *
     * @return Returns the value of <code>uio_td</code> field of <code>uio</code> structure.
     */
    public Pointer getOwner() {
        return Pointer.valueOf(this.ptr.read8(OFFSET_OWNER));
    }

    /**
     * Make sure to free the UioType buffer during garbage collection.
     *
     * @throws Throwable If finalization failed.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            free();
        } finally {
            super.finalize();
        }
    }

    /**
     * Frees the native memory needed for the UioType, if it was allocated by the constructor.
     * After <code>free()</code> is called on such UioType,
     * using this Java wrapper instance will no longer be possible.
     */
    public void free() {
        if (this.ownPtr && this.ptr != null && this.ptr.addr() != 0) {
            this.ptr.free();
        }
    }

    /**
     * Gets the native memory pointer where this UioType's data is stored.
     *
     * @return UioType memory pointer.
     */
    public Pointer getPointer() {
        return this.ptr;
    }
}

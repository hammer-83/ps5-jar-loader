package org.ps5jb.sdk.include;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/unistd.h</code> from FreeBSD source.
 */
public class UniStd {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public UniStd(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int getuid() {
        return libKernel.getuid();
    }

    public void setuid(int uid) throws OperationNotPermittedException {
        int ret = libKernel.setuid(uid);
        if (ret == -1) {
            SdkException ex = errNo.getLastException(getClass(), "setuid");
            if (ex instanceof OperationNotPermittedException) {
                throw (OperationNotPermittedException) ex;
            } else {
                throw new SdkRuntimeException(ex.getMessage(), ex);
            }
        }
    }
}

package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.cpuset.CpuLevelType;
import org.ps5jb.sdk.include.sys.cpuset.CpuSetType;
import org.ps5jb.sdk.include.sys.cpuset.CpuWhichType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/cpuset.h</code> from FreeBSD source.
 */
public class CpuSet {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public CpuSet(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public void setAffinity(CpuLevelType cpuLevel, CpuWhichType cpuWhich, int id, CpuSetType cpuSetType) throws SdkException {
        int ret = libKernel.cpuset_setaffinity(cpuLevel.value(), cpuWhich.value(), id, cpuSetType.getSize(), cpuSetType.getPointer());
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "setAffinity");
        }
    }
}

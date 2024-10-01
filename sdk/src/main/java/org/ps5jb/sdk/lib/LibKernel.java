package org.ps5jb.sdk.lib;

import org.ps5jb.sdk.core.Library;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * <p>
 * Low-level interface to functions in <code>libkernel</code> library.
 * The functions in this class do direct native calls and return direct
 * result without any Java-friendly conversion.
 * </p>
 * <p>
 * It is advisable to avoid using this class directly and instead
 * go through FreeBSD-style wrappers in {@link org.ps5jb.sdk.include} package.
 * Those functions provide type-safe signatures and do proper error handling.
 * </p>
 * <p>
 * This class is not thread-safe and each thread should instantiate its own
 * instance.
 * </p>
 */
public class LibKernel extends Library {
    private Pointer __error;
    private Pointer cpuset_getaffinity;
    private Pointer cpuset_setaffinity;
    private Pointer sceKernelSendNotificationRequest;
    private Pointer getuid;
    private Pointer setuid;
    private Pointer getpid;
    private Pointer open;
    private Pointer close;
    private Pointer getdents;
    private Pointer stat;
    private Pointer fstat;
    private Pointer sceKernelCheckReachability;
    private Pointer pthread_rename_np;
    private Pointer pthread_self;
    private Pointer rtprio_thread;
    private Pointer pipe;
    private Pointer shm_open;
    private Pointer shm_unlink;
    private Pointer mmap;
    private Pointer munmap;
    private Pointer ftruncate;
    private Pointer select;
    private Pointer ioctl;
    private Pointer read;
    private Pointer write;
    private Pointer _umtx_op;
    private Pointer mprotect;
    private Pointer sched_yield;
    private Pointer sceKernelGetCurrentCpu;
    private Pointer sceKernelGetProsperoSystemSwVersion;

    /**
     * Constructor.
     */
    public LibKernel() {
        super(0x2001);
    }

    public int sceKernelSendNotificationRequest(String msg) {
        final long size = 0xC30;
        Pointer buf = Pointer.calloc(size);
        try {
            buf.write4(0x10, -1);
            buf.inc(0x2D).writeString(msg);

            if (sceKernelSendNotificationRequest == null) {
                sceKernelSendNotificationRequest = addrOf("sceKernelSendNotificationRequest");
            }

            return (int) call(sceKernelSendNotificationRequest, 0, buf.addr(), size, 0);
        } finally {
            buf.free();
        }
    }

    /**
     * Retrieves the mask from the object specified by <code>level</code>, <code>which</code> and <code>id</code>
     * and stores it in the space provided by <code>mask</code>.
     *
     * @param level One of {@link org.ps5jb.sdk.include.sys.cpuset.CpuLevelType} values.
     *   See FreeBSD documentation for possible combinations of <code>level</code> and <code>which</code>.
     * @param which One of {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType} values.
     *   See FreeBSD documentation for possible combinations of <code>level</code> and <code>which</code>.
     * @param id The id of -1 may be used with a <code>which</code> of
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_TID CPU_WHICH_TID},
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_PID CPU_WHICH_PID}, or
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_CPUSET CPU_WHICH_CPUSET}
     *   to mean the current thread, process, or current thread's cpuset.
     * @param setsize Size of the native memory allocated by {@link org.ps5jb.sdk.include.sys.cpuset.CpuSetType}.
     * @param mask Pointer to the native memory allocated by {@link org.ps5jb.sdk.include.sys.cpuset.CpuSetType}.
     * @return Upon successful completion, the value 0 is returned; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int cpuset_getaffinity(int level, int which, long id, long setsize, Pointer mask) {
        if (cpuset_getaffinity == null) {
            cpuset_getaffinity = addrOf("cpuset_getaffinity");
        }
        return (int) call(cpuset_getaffinity, level, which, id, setsize, mask.addr());
    }

    /**
     * Manipulate the sets of CPUs available to processes, threads, interrupts, jails and other resources.
     * These functions may manipulate sets of CPUs that contain many processes or per-object anonymous masks that
     * effect only a single object.
     *
     * @param level One of {@link org.ps5jb.sdk.include.sys.cpuset.CpuLevelType} values.
     *   See FreeBSD documentation for possible combinations of <code>level</code> and <code>which</code>.
     * @param which One of {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType} values.
     *   See FreeBSD documentation for possible combinations of <code>level</code> and <code>which</code>.
     * @param id The id of -1 may be used with a <code>which</code> of
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_TID CPU_WHICH_TID},
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_PID CPU_WHICH_PID}, or
     *   {@link org.ps5jb.sdk.include.sys.cpuset.CpuWhichType#CPU_WHICH_CPUSET CPU_WHICH_CPUSET}
     *   to mean the current thread, process, or current thread's cpuset.
     * @param setsize Size of the native memory allocated by {@link org.ps5jb.sdk.include.sys.cpuset.CpuSetType}.
     * @param mask Pointer to the native memory allocated by {@link org.ps5jb.sdk.include.sys.cpuset.CpuSetType}.
     * @return Upon successful completion, the value 0 is returned; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int cpuset_setaffinity(int level, int which, long id, long setsize, Pointer mask) {
        if (cpuset_setaffinity == null) {
            cpuset_setaffinity = addrOf("cpuset_setaffinity");
        }
        return (int) call(cpuset_setaffinity, level, which, id, setsize, mask.addr());
    }

    /**
     * Return a pointer to a field in the thread-specific structure for threads other than
     * the initial thread. For the initial theread and non-threaded processes, <code>__errno()</code>
     * returns a pointer to a global <code>errno</code> variable that is compatible with the previous
     * definition.
     * When a system call detects an error, it returns an integer value indicating faulure (usually -1)
     * and sets the variable <code>errno</code> accordingly. Successful calls never set <code>errno</code>;
     * once set it remains unil another error occurs. it should only be examined after an error.
     *
     * @return Pointer to the <code>errno</code> variable.
     */
    public Pointer __error() {
        if (__error == null) {
            __error = addrOf("__error");
        }
        return Pointer.valueOf(call(__error));
    }

    /**
     * Return the real user ID of the calling process. The real user ID the that of the user
     * who has invoked the program.
     *
     * @return Real user ID.
     */
    public int getuid() {
        if (getuid == null) {
            getuid = addrOf("getuid");
        }
        return (int) call(getuid);
    }

    /**
     * Set the real and effective user IDs and the saved set-user-ID of the current process to
     * the specified value. This system call is permitted if the specified ID is equal to the
     * real user ID or the effective user ID of the process, or if the effective user ID is that
     * of the super user.
     *
     * @param uid New user ID.
     * @return Upon successful completion, the value 0 is returned; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int setuid(int uid) {
        if (setuid == null) {
            setuid = addrOf("setuid");
        }
        return (int) call(setuid, uid);
    }

    /**
     * Return the process ID of the calling process.
     *
     * @return Process ID
     */
    public int getpid() {
        if (getpid == null) {
            getpid = addrOf("getpid");
        }
        return (int) call(getpid);
    }

    /**
     * Open or create a file for reading, writing or executing.
     *
     * @param path File name to open.
     * @param flags One or more flags determining the mode of the file opening.
     * @return If successful, return a non-negative integer, termined a file descriptor.
     *   Return -1 on failer, and set {@link ErrNo#errno() errno} to indicate the error.
     */
    public int open(String path, int flags) {
        if (open == null) {
            open = addrOf("open");
        }

        Pointer buf = Pointer.fromString(path);
        try {
            return (int) call(open, buf.addr(), flags);
        } finally {
            buf.free();
        }
    }

    /**
     * Delete a descriptor.
     *
     * @param fd Descriptor to delete.
     * @return Return the value 0 if successful; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int close(int fd) {
        if (close == null) {
            close = addrOf("close");
        }
        return (int) call(close, fd);
    }

    /**
     * Get directory entries in a file system independent format.
     *
     * @param fd File descriptor of the directory whose entries to read.
     * @param buf Buffer where the directory entries will be stored.
     * @param nbytes Up to nbytes of data will be transferred.
     *   This value must be greater than or equal to the block size associated with the file.
     *   See {@link #stat(String, Pointer)}.
     * @return If successful, the number of bytes actually transferred is returned.
     *    Otherwise the value -1 is returned and the global variable
     *    {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int getdents(int fd, Pointer buf, long nbytes) {
        if (getdents == null) {
            getdents = addrOf("getdents");
        }
        return (int) call(getdents, fd, buf.addr(), nbytes);
    }

    /**
     * Get file status.
     *
     * @param path Path of the file whose status to obtain.
     * @param sb Buffer where the status is stored.
     * @return Upon successful completion, the value 0 is returned; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int stat(String path, Pointer sb) {
        if (stat == null) {
            stat = addrOf("stat");
        }

        Pointer buf = Pointer.fromString(path);
        try {
            return (int) call(stat, buf.addr(), sb.addr());
        } finally {
            buf.free();
        }
    }

    /**
     * Get status of an open file known by the file descriptor.
     *
     * @param fd File descriptor.
     * @param sb Buffer where the status is stored.
     * @return Upon successful completion, the value 0 is returned; otherwise the value -1 is returned
     *   and the global variable {@link ErrNo#errno() errno} is set to indicate the error.
     */
    public int fstat(int fd, Pointer sb) {
        if (fstat == null) {
            fstat = addrOf("fstat");
        }

        return (int) call(fstat, fd, sb.addr());
    }

    public int sceKernelCheckReachability(String path) {
        if (sceKernelCheckReachability == null) {
            sceKernelCheckReachability = addrOf("sceKernelCheckReachability");
        }

        Pointer buf = Pointer.fromString(path);
        try {
            return (int) call(sceKernelCheckReachability, buf.addr());
        } finally {
            buf.free();
        }
    }

    public int pthread_rename_np(Pointer thread, String name) {
        if (pthread_rename_np == null) {
            pthread_rename_np = addrOf("pthread_rename_np");
        }

        Pointer buf = Pointer.fromString(name);
        try {
            return (int) call(pthread_rename_np, thread.addr(), buf.addr());
        } finally {
            buf.free();
        }
    }

    public Pointer pthread_self() {
        if (pthread_self == null) {
            pthread_self = addrOf("pthread_self");
        }

        return Pointer.valueOf(call(pthread_self));
    }

    public int rtprio_thread(int function, int lwpid, Pointer rtprio) {
        if (rtprio_thread == null) {
            rtprio_thread = addrOf("rtprio_thread");
        }

        return (int) call(rtprio_thread, function, lwpid, rtprio.addr());
    }

    public int pipe(Pointer fildes) {
        if (pipe == null) {
            pipe = addrOf("pipe");
        }

        return (int) call(pipe, fildes.addr());
    }

    public int shm_open(Pointer path, int flags, int mode) {
        if (shm_open == null) {
            shm_open = addrOf("shm_open");
        }

        return (int) call(shm_open, path.addr(), flags, mode);
    }

    public int shm_unlink(Pointer path) {
        if (shm_unlink == null) {
            shm_unlink = addrOf("shm_unlink");
        }

        return (int) call(shm_unlink, path.addr());
    }

    public Pointer mmap(Pointer addr, long len, int prot, int flags, int fd, long offset) {
        if (mmap == null) {
            mmap = addrOf("mmap");
        }

        return new Pointer(call(mmap, addr.addr(), len, prot, flags, fd, offset), new Long(len));
    }

    public int munmap(Pointer addr, long len) {
        if (munmap == null) {
            munmap = addrOf("munmap");
        }

        return (int) call(munmap, addr.addr(), len);
    }

    public int ftruncate(int fd, long length) {
        if (ftruncate == null) {
            ftruncate = addrOf("ftruncate");
        }

        return (int) call(ftruncate, fd, length);
    }

    public int select(int nfds, Pointer readfds, Pointer writefds, Pointer exceptfds, Pointer timeout) {
        if (select == null) {
            select = addrOf("select");
        }

        return (int) call(select, nfds, readfds.addr(), writefds.addr(), exceptfds.addr(), timeout.addr());
    }

    public int ioctl(int fd, long request, long argp) {
        if (ioctl == null) {
            ioctl = addrOf("ioctl");
        }

        return (int) call(ioctl, fd, request, argp);
    }

    public long read(int fd, Pointer buf, long nbytes) {
        if (read == null) {
            read = addrOf("read");
        }

        return call(read, fd, buf.addr(), nbytes);
    }

    public long write(int fd, Pointer buf, long nbytes) {
        if (write == null) {
            write = addrOf("write");
        }

        return call(write, fd, buf.addr(), nbytes);
    }

    public int _umtx_op(Pointer obj, int op, long val, Pointer uaddr, Pointer uaddr2) {
        if (_umtx_op == null) {
            _umtx_op = addrOf("_umtx_op");
        }

        return (int) call(_umtx_op, obj.addr(), op, val, uaddr.addr(), uaddr2.addr());
    }

    public int mprotect(Pointer addr, long len, int prot) {
        if (mprotect == null) {
            mprotect = addrOf("mprotect");
        }

        return (int) call(mprotect, addr.addr(), len, prot);
    }

    public int sched_yield(long unused) {
        if (sched_yield == null) {
            sched_yield = addrOf("sched_yield");
        }

        return (int) call(sched_yield, unused);
    }

    public int sceKernelGetCurrentCpu() {
        if (sceKernelGetCurrentCpu == null) {
            sceKernelGetCurrentCpu = addrOf("sceKernelGetCurrentCpu");
        }
        return (int) call(sceKernelGetCurrentCpu);
    }

    /**
     * Retrieve the PS5 system software version information.
     *
     * @return Buffer with the version information.
     * @throws org.ps5jb.sdk.core.SdkRuntimeException If an error occurred while retrieving the version.
     */
    public byte[] sceKernelGetProsperoSystemSwVersion() {
        final long size = 0x28;
        Pointer buf = Pointer.calloc(size);
        try {
            buf.write8(size);

            if (sceKernelGetProsperoSystemSwVersion == null) {
                sceKernelGetProsperoSystemSwVersion = addrOf("sceKernelGetProsperoSystemSwVersion");
            }

            int ret = (int) call(sceKernelGetProsperoSystemSwVersion, buf.addr());

            if (ret != 0) {
                throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(LibKernel.class, "sceKernelGetProsperoSystemSwVersion", "0x" + Integer.toHexString(ret)));
            }

            final long resultOffset = 0x8L;
            final int resultSize = (int) (size - resultOffset);
            byte[] result = new byte[resultSize];
            buf.read(resultOffset, result, 0, resultSize);
            return result;
        } finally {
            buf.free();
        }
    }
}

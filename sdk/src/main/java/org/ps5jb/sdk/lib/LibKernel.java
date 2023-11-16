package org.ps5jb.sdk.lib;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Library;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.ErrNo;

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
    private Pointer cpuset_setaffinity;
    private Pointer sceKernelSendNotificationRequest;
    private Pointer getuid;
    private Pointer setuid;
    private Pointer open;
    private Pointer close;
    private Pointer getdents;
    private Pointer stat;
    private Pointer sceKernelCheckReachability;

    /**
     * Constructor.
     */
    public LibKernel() {
        super(0x2001);
    }

    public int sceKernelSendNotificationRequest(String msg) {
        long size = 0xC30;
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
     *
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
}

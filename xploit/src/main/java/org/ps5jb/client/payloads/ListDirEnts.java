package org.ps5jb.client.payloads;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.FCntl;
import org.ps5jb.sdk.include.sys.dirent.DirEnt;
import org.ps5jb.sdk.include.sys.dirent.DirType;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This sample uses native code execution to list accessible entries starting from root directory.
 * Since the PS5 screen output is rather small, it's advisable to properly configure
 * the JAR loader with a {@link org.ps5jb.loader.RemoteLogger remote logger} which would
 * allow to capture all the textual screen output over network.
 */
public class ListDirEnts implements Runnable {
    /**
     * Use native calls from "dirent" module to list the available filesystem entries.
     */
    public void run() {
        try {
            LibKernel libKernel = new LibKernel();
            FCntl fcntl = new FCntl(libKernel);
            try {
                printDirEnts("/", "", libKernel, fcntl);
            } finally {
                libKernel.closeLibrary();
            }
        } catch (SdkException e) {
            Status.printStackTrace(e.getMessage(), e);
        }
    }

    public void getDirEnts(List dirEnts, String path, LibKernel libKernel, FCntl fcntl, boolean recurse) throws SdkException {
        if (libKernel.sceKernelCheckReachability(path) == 0) {
            int fd = fcntl.open(path, OpenFlag.O_RDONLY, OpenFlag.O_DIRECTORY);
            try {
                File root = new File(path);
                int BUF_SIZE = 16 * 1024;
                int remainingSize = BUF_SIZE;
                Pointer db = Pointer.malloc(BUF_SIZE);
                try {
                    DirEnt dirEnt = null;
                    remainingSize = libKernel.getdents(fd, db, BUF_SIZE);
                    while (remainingSize > 0 && remainingSize <= BUF_SIZE) {
                        if (dirEnt == null) {
                            dirEnt = new DirEnt(db);
                        }
                        if (!dirEnt.getName().equals(".") && !dirEnt.getName().equals("..")) {
                            dirEnts.add(new File(root, dirEnt.getName()));

                            if (recurse && DirType.DT_DIR.equals(dirEnt.getDirType())) {
                                String childPath = path + (path == "/" ? "" : "/") + dirEnt.getName();
                                try {
                                    getDirEnts(dirEnts, childPath, libKernel, fcntl, recurse);
                                } catch (SdkException e) {
                                    Status.println("[ERROR] " + e.getMessage());
                                }
                            }
                        }

                        long oldAddr = dirEnt.getPointer().addr();
                        dirEnt = dirEnt.next(remainingSize);
                        if (dirEnt == null) {
                            remainingSize = libKernel.getdents(fd, db, BUF_SIZE);
                        } else {
                            remainingSize -= dirEnt.getPointer().addr() - oldAddr;
                        }
                    }
                } finally {
                    db.free();
                }
            } finally {
                fcntl.close(fd);
            }
        }
    }

    private void printDirEnts(String path, String indent, LibKernel libKernel, FCntl fcntl) throws SdkException {
        List dirEnts = new ArrayList();
        try {
            getDirEnts(dirEnts, path, libKernel, fcntl, false);
        } catch (SdkException e) {
            Status.println(indent + "  [ERROR] " + e.getMessage());
        }

        Iterator dirEntsIter = dirEnts.iterator();
        while (dirEntsIter.hasNext()) {
            File file = (File) dirEntsIter.next();
            String childPath = file.getAbsolutePath();
            Status.println(indent + file.getName() + (file.isDirectory() ? " [DIR]" : ""));
            if (file.isDirectory()) {
                printDirEnts(childPath, indent + "  ", libKernel, fcntl);
            }
        }
    }
}

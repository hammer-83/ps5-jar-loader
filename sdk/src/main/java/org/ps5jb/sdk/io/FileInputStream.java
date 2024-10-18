package org.ps5jb.sdk.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.lib.LibKernel;

public class FileInputStream extends java.io.FileInputStream {
    public FileInputStream(String name) throws FileNotFoundException {
        super(new FileDescriptor());
        disableProxies();
        openFile(new java.io.File(name));
    }

    public FileInputStream(java.io.File file) throws FileNotFoundException {
        super(new FileDescriptor());
        disableProxies();
        openFile(file);
    }

    public FileInputStream(FileDescriptor fileDescriptor) {
        super(fileDescriptor);
        disableProxies();
    }

    private void disableProxies() {
        try {
            Field proxyField = java.io.FileInputStream.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(this, null);

            FileDescriptor fd = getFd();
            proxyField = java.io.FileDescriptor.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(fd, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }
    }

    private FileDescriptor getFd() {
        try {
            Field fdField = java.io.FileInputStream.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            return (FileDescriptor) fdField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }
    }

    private void openFile(java.io.File file) throws FileNotFoundException {
        LibKernel libKernel = new LibKernel();
        int fd = libKernel.open(file.getAbsolutePath(), OpenFlag.or(OpenFlag.O_RDONLY));
        if (fd == -1) {
            throw new FileNotFoundException();
        }

        try {
            Method setMethod = java.io.FileDescriptor.class.getDeclaredMethod("set", new Class[] { int.class });
            setMethod.setAccessible(true);
            setMethod.invoke(getFd(), new Object[] { new Integer(fd) });
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException());
        }
    }
}

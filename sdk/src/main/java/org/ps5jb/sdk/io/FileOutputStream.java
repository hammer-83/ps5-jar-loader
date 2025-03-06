package org.ps5jb.sdk.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import org.ps5jb.sdk.core.SdkRuntimeException;

public class FileOutputStream extends java.io.FileOutputStream {
    public FileOutputStream(String name) throws FileNotFoundException {
        super(name);
        disableProxies();
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        super(name, append);
        disableProxies();
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        super(file);
        disableProxies();
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        super(file, append);
        disableProxies();
    }

    public FileOutputStream(FileDescriptor fileDescriptor) {
        super(fileDescriptor);
        disableProxies();
    }

    private void disableProxies() {
        try {
            Field proxyField = java.io.FileOutputStream.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(this, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }

        FileDescriptor fd = getFd();
        FileDescriptorFactory.disableFileDescriptorProxy(fd);
    }

    public FileDescriptor getFd() {
        try {
            Field fdField = java.io.FileOutputStream.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            return (FileDescriptor) fdField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new SdkRuntimeException(e);
        }
    }
}

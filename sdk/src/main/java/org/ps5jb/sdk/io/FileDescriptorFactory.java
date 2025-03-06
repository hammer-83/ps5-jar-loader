package org.ps5jb.sdk.io;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import org.ps5jb.sdk.core.SdkRuntimeException;

/**
 * Factory to create instances of {@link FileDescriptor} classes and
 * call its protected methods
 */
public class FileDescriptorFactory {
    /**
     * Private constructor. All methods in this class are static.
     */
    private FileDescriptorFactory() {
    }

    /**
     * Create a new instance of FileDescriptor with a given fd value.
     *
     * @param fd File descriptor number from a native call.
     * @return FileDescriptor instance.
     * @throws SdkRuntimeException If an error occurs while creating the FileDescriptor instance.
     */
    public static FileDescriptor createFileDescriptor(int fd) {
        try {
            Constructor constr = FileDescriptor.class.getDeclaredConstructor(new Class[] { int.class });
            constr.setAccessible(true);
            FileDescriptor result = (FileDescriptor) constr.newInstance(new Object[] { new Integer(fd) });
            disableFileDescriptorProxy(result);
            return result;
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Disables a BD-J proxy of a FileDescriptor instance.
     *
     * @param fileDescriptor FileDescriptor instance whose proxy to disable.
     * @throws SdkRuntimeException If an error occurs while disabling the proxy.
     */
    public static void disableFileDescriptorProxy(FileDescriptor fileDescriptor) {
        try {
            Field proxyField = java.io.FileDescriptor.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(fileDescriptor, null);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns the file descriptor value suitable for native calls.
     *
     * @param fileDescriptor FileDescriptor instance whose fd to return.
     * @return Integer that can be used in native calls to do I/O with
     *   this file descriptor.
     */
    public static int getFd(FileDescriptor fileDescriptor) {
        try {
            Field fdField = java.io.FileDescriptor.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            return fdField.getInt(fileDescriptor);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    public static FileDescriptor getSocketFileDescriptor(Socket socket) {
        return getSocketFileDescriptor(Socket.class, socket);
    }

    public static FileDescriptor getSocketFileDescriptor(ServerSocket socket) {
        return getSocketFileDescriptor(ServerSocket.class, socket);
    }

    private static FileDescriptor getSocketFileDescriptor(Class socketClass, Object socket) {
        try {
            Method getImplMethod = socketClass.getDeclaredMethod("getImpl", new Class[0]);
            getImplMethod.setAccessible(true);
            SocketImpl socketImpl = (SocketImpl) getImplMethod.invoke(socket, new Object[0]);

            Field socketFdField = SocketImpl.class.getDeclaredField("fd");
            socketFdField.setAccessible(true);
            return (FileDescriptor) socketFdField.get(socketImpl);
        } catch (InvocationTargetException e) {
            throw new SdkRuntimeException(e.getTargetException().getMessage(), e.getTargetException());
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }
}

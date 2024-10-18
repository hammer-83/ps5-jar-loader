package org.ps5jb.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Thread that creates a server socket and listens for the incoming connections sending the JARs for execution.
 */
public class JarLoader extends SocketListener {
    /**
     * JarLoader constructor.
     *
     * @param port Port number on which to listen for incoming JARs.
     * @throws IOException When server socket constructor throws this exception.
     * @see ServerSocket#ServerSocket(int)
     */
    public JarLoader(int port) throws IOException {
        super("JAR Loader", port);
    }

    /**
     * Called when a client connection sending a JAR file is accepted.
     *
     * @param clientSocket Client socket.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Override
    protected void acceptClient(Socket clientSocket) throws IOException, ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        InputStream jarStream = clientSocket.getInputStream();
        try {
            // Generate a unique JAR filename in the temporary directory.
            Path jarPath = Files.createTempFile("jarLoader", ".jar");
            File jarFile = jarPath.toFile();
            try {
                // Delete temp jar on exit if it is not cleaned up manually for some reason
                jarFile.deleteOnExit();

                // Receive JAR data and save it to temporary file
                Status.println("Receiving JAR data to: " + jarFile);
                byte[] buf = new byte[8192];
                int readCount;
                int totalSize = 0;
                OutputStream jarOut = Files.newOutputStream(jarPath);
                try {
                    while ((readCount = jarStream.read(buf)) != -1) {
                        jarOut.write(buf, 0, readCount);

                        Status.println("Received " + totalSize + " bytes...", totalSize != 0);
                        totalSize += readCount;
                    }
                } finally {
                     jarOut.close();
                }
                Status.println("Received " + totalSize + " bytes...Done", true);
                Status.println("Reading JAR Manifest...");

                // Read JAR manifest to get the main class
                String mainClassName;
                JarFile jar = new JarFile(jarFile);
                try {
                    JarEntry manifestEntry = jar.getJarEntry("META-INF/MANIFEST.MF");
                    if (manifestEntry == null) {
                        throw new FileNotFoundException("Unable to find JAR manifest");
                    }

                    InputStream manifestStream = jar.getInputStream(manifestEntry);
                    try {
                        Manifest mf = new Manifest(manifestStream);
                        mainClassName = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                        if (mainClassName == null) {
                            throw new ClassNotFoundException("Main class not defined in the JAR");
                        }
                    } finally {
                        manifestStream.close();
                    }
                } finally {
                    jar.close();
                }
                Status.println("Reading JAR Manifest...Main Class: " + mainClassName, true);

                // Load the JAR in a new classloader and execute the main method.
                // When classloader is garbage collected, all classes will be unloaded as well.
                // This is important to be able to load the modified copy of the same class on the next iteration.
                Status.println("Loading the JAR...");

                java.net.URLClassLoader ldr = java.net.URLClassLoader.newInstance(new java.net.URL[] { jarPath.toUri().toURL() }, getClass().getClassLoader());
                Class mainClass = ldr.loadClass(mainClassName);
                Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
                mainMethod.invoke(null, new Object[] { new String[0] });
            } finally {
                // Delete the file containing the temporary JAR
                if (!jarFile.delete()) {
                    // Assume temp path changed by JAR. Try the new temp path
                    boolean displaced = false;
                    String tempPath = System.getProperty("java.io.tmpdir");
                    if (jarFile.getAbsolutePath().indexOf(tempPath) != 0) {
                        jarFile = new File(tempPath, jarFile.getName());
                        if (jarFile.delete()) {
                            displaced = true;
                        }
                    }

                    if (!displaced) {
                        Status.println("Failed to delete the temporary JAR");
                    }
                }
            }
        } finally {
            jarStream.close();
        }
    }

    /**
     * Handles exceptions declared as throwable by {@link #acceptClient(Socket)} by
     * printing a status message and a stack trace.
     *
     * @param ex Handled exception.
     */
    @Override
    protected void handleException(Throwable ex) {
        if (ex instanceof InvocationTargetException) {
            Status.printStackTrace("Execution of JAR threw an exception", ((InvocationTargetException) ex).getTargetException());
        } else if (ex instanceof NoSuchMethodException) {
            Status.printStackTrace("Main class does not contain 'main(String[])' method", ex);
        } else {
            super.handleException(ex);
        }
    }
}

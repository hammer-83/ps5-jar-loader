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
public class JarLoader implements Runnable {
    private boolean terminated;
    private ServerSocket serverSocket;
    private String netAddress;

    /**
     * JarLoader constructor.
     *
     * @param port Port number on which to listen for incoming JARs.
     * @throws IOException When server socket constructor throws this exception.
     * @see ServerSocket#ServerSocket(int)
     */
    public JarLoader(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(5000);

        // Determine network interface address to print it on screen
        final DatagramSocket tempSocket = new DatagramSocket();
        try {
            tempSocket.setSoTimeout(1000);
            tempSocket.connect(InetAddress.getByName("8.8.8.8"), 53);
            netAddress = tempSocket.getLocalAddress().getHostAddress();
        } catch (Throwable e) {
            // Ignore timeout, network address will remain undetermined
            Status.println("Warning, IP address could not be determined. Exception: " + e.getClass().getName() + ". Message: " + e.getMessage());
        } finally {
            tempSocket.close();
        }
    }

    /**
     * JarLoader thread entry point.
     */
    @Override
    public void run() {
        boolean printWaiting = true;

        while (!terminated) {
            try {
                if (printWaiting) {
                    if (netAddress == null) {
                        Status.println("Waiting for JAR on port " + serverSocket.getLocalPort() +
                                ", but the IPv4 address could not be determined. Double-check the network connectivity...");
                    } else {
                        Status.println("Waiting for JAR on " + netAddress + ":" + serverSocket.getLocalPort() + "...");
                    }
                }
                printWaiting = true;

                // Listen for new incoming connection
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(5000);
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
                            Status.println("Failed to delete the temporary JAR");
                        }
                    }
                } finally {
                    jarStream.close();
                }
            } catch (InterruptedIOException e) {
                // Do nothing, this is expected due to socket timeout.
                // Listening will just restart until terminate() is called.
                printWaiting = false;
            } catch (InvocationTargetException e) {
                Status.printStackTrace("Execution of JAR threw an exception", e.getTargetException());
            } catch (NoSuchMethodException e) {
                Status.printStackTrace("Main class does not contain 'main(String[])' method", e);
            } catch (IOException | ClassNotFoundException | IllegalAccessException | RuntimeException | Error e) {
                Status.printStackTrace("Error occurred", e);
            }
        }
        Status.println("JarLoader terminated!");
    }

    /**
     * Call to signal the thread to terminate the listening. This is not an immediate termination, it may take a few
     * moments for the thread to be actually done. To wait on it, make sure to call {@link Thread#join()} on this thread
     * after calling {@code terminate()}.
     *
     * @throws IOException If socket closure throws this exception.
     */
    public void terminate() throws IOException {
        terminated = true;
        serverSocket.close();
    }
}

package org.ps5jb.client;

import jdk.internal.loader.URLClassPath;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Main class of the JAR that will be executed remotely on the PS5. It has two roles:
 * <ol>
 *     <li>Utility to send the whole JAR to the PS5 when {@link #main(String[])} is invoked with specific parameters.</li>
 *     <li>Execution of {@link Exploit#execute()} when {@code main(String[])} is called with no parameters (presumably by the JAR loading Xlet).</li>
 * </ol>
 */
public class JarMain {
    /**
     * Sends the jar to the remote host
     *
     * @param hostArg Hostname or IP address of the machine receiving the JAR.
     * @param portArg Port on which the machine is listening for the connection.
     * @param jarPath Path to the JAR file to send.
     * @throws IOException If I/O exception occurs during sending.
     */
    protected static void sendJar(String hostArg, String portArg, Path jarPath) throws IOException {
        InetAddress addr = InetAddress.getByName(hostArg);
        int port = Integer.parseInt(portArg);

        Socket socket = new Socket(addr, port);
        socket.setSoTimeout(5000);

        InputStream jarStream = Files.newInputStream(jarPath);
        try {
            OutputStream out = socket.getOutputStream();
            try {
                byte[] buf = new byte[8192];
                int readCount;
                while ((readCount = jarStream.read(buf)) != -1) {
                    out.write(buf, 0, readCount);
                }

                System.out.println("JAR successfully sent");
            } finally {
                out.close();
            }
        } finally {
            jarStream.close();
        }
    }

    /**
     * Uses the class classloader and reflection to determine the path to own JAR file.
     *
     * @return Path to the JAR file containing this class. Returns {@code null} if JAR path could not be determined.
     * @throws NoSuchFieldException If the class classloader does not have "ucp" field. The classloader is assumed to
     *   be a subclass of {@link jdk.internal.loader.BuiltinClassLoader}.
     * @throws IllegalAccessException If access to the classloader state is denied through reflection.
     * @throws IOException If I/O error occurs while reading the JAR file.
     */
    protected static Path discoverJar() throws NoSuchFieldException, IllegalAccessException, IOException {
        Path jarPath = null;

        // Try to determine the JAR file from classloader
        ClassLoader cl = JarMain.class.getClassLoader();
        Field field = cl.getClass().getDeclaredField("ucp");
        field.setAccessible(true);
        URLClassPath ucp = (URLClassPath) field.get(cl);
        for (URL url : ucp.getURLs()) {
            if (url.getProtocol().equals("file")) {
                Path path;
                try {
                    path = Path.of(new URI(url.toString()));
                } catch (URISyntaxException e) {
                    // Should not happen in practice
                    throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
                }
                JarFile jar = new JarFile(path.toFile());
                String classEntryName = JarMain.class.getName().replace('.', '/') + ".class";
                JarEntry jarEntry = jar.getJarEntry(classEntryName);
                if (jarEntry != null) {
                    jarPath = path;
                    break;
                }
            }
        }

        return jarPath;
    }

    /**
     * JAR entry point. Executed to send the JAR to the PS5 and to run the {@link Exploit} code remotely.
     *
     * @param args Command-line arguments.
     * @throws Exception Any exception in {@code main(String[])} will be rethrown.
     */
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            // No args => execute exploit
            (new Exploit()).execute();
        } else if (args.length > 0 && args[0].equals("--help")) {
            // First arg is help, show usage
            System.out.println("Usage: java --add-opens java.base/jdk.internal.loader=ALL-UNNAMED -jar xploit.jar <address> [<port>]");
        } else {
            // Any other case, attempt to send the jar
            String host = args[0];
            String port = args.length > 1 ? args[1] : "9025";

            Path jarPath = discoverJar();
            if (jarPath == null) {
                throw new FileNotFoundException("Jar file path could not be determined from the classloader");
            }

            sendJar(host, port, jarPath);
        }
    }
}

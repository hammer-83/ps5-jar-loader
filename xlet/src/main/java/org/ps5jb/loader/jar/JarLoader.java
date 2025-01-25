package org.ps5jb.loader.jar;

import org.ps5jb.loader.Status;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public interface JarLoader extends Runnable {

    default void loadJar(final File jarFile) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
        Status.println("Reading JAR Manifest... Main Class: " + mainClassName, true);

        // Load the JAR in a new classloader and execute the main method.
        // When classloader is garbage collected, all classes will be unloaded as well.
        // This is important to be able to load the modified copy of the same class on the next iteration.
        Status.println("Loading the JAR...");

        java.net.URLClassLoader ldr = java.net.URLClassLoader.newInstance(new java.net.URL[] { jarFile.toURL() }, getClass().getClassLoader());
        Class mainClass = ldr.loadClass(mainClassName);
        Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
        mainMethod.invoke(null, new Object[] { new String[0] });
    }

    default void loadJar(final Path jarPath) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        this.loadJar(jarPath.toFile());
    }

    void terminate() throws IOException;
}

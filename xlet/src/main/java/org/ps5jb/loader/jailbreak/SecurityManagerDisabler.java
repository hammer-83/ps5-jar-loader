package org.ps5jb.loader.jailbreak;

import java.net.MalformedURLException;
import java.net.URLClassLoader;

import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.access.SharedSecrets;

/**
 * This class executes a vulnerability that disables the security manager and allows full access to the Java platform on PS5.
 */
public class SecurityManagerDisabler {
    /**
     * Execute the disabling of the security manager.
     *
     * @return True if security manager has been successfully disabled.
     */
    public static boolean execute() {
        JavaSecurityAccess real = SharedSecrets.getJavaSecurityAccess();
        JavaSecurityProxy fake = new JavaSecurityProxy(real);
        SharedSecrets.setJavaSecurityAccess(fake);
        try {
            URLClassLoader ldr = URLClassLoader.newInstance(new java.net.URL[] { new java.net.URL("file:///VP/BDMV/JAR/00000.jar") });
            ldr.loadClass(DisableSecurityManagerAction.class.getName()).newInstance();
        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new InternalError(e.getClass().getName() + ": " + e.getMessage());
        } finally {
            SharedSecrets.setJavaSecurityAccess(real);
        }

        return System.getSecurityManager() == null;
    }
}

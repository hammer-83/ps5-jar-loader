package org.ps5jb.loader.jailbreak;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Payload class that is loaded with all permissions in order to disable the security manager.
 */
public class DisableSecurityManagerAction implements PrivilegedExceptionAction {
    public DisableSecurityManagerAction() throws PrivilegedActionException {
        AccessController.doPrivileged(this);
    }

    @Override
    public Object run() {
        System.setSecurityManager(null);
        return null;
    }
}
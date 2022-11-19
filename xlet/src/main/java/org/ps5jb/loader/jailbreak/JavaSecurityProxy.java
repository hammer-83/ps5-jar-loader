package org.ps5jb.loader.jailbreak;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.util.Set;
import jdk.internal.access.JavaSecurityAccess;
import jdk.internal.security.CodeSigner;
import jdk.internal.security.Entry;
import jdk.internal.security.ProtectionParameter;
import jdk.internal.security.Service;
import jdk.internal.security.keystore.Builder;

/**
 * Proxy for the real instance of {@code JavaSecurityAccess}. By "tweaking" certain methods, the classes end up
 * loaded with escalated permissions.
 */
public class JavaSecurityProxy implements jdk.internal.access.JavaSecurityAccess {
    private final JavaSecurityAccess real;

    public JavaSecurityProxy(JavaSecurityAccess real) {
        this.real = real;
    }

    @Override
    public Object doIntersectionPrivilege(PrivilegedAction a, AccessControlContext b, AccessControlContext c) {
        return real.doIntersectionPrivilege(a, b, c);
    }

    @Override
    public Object doIntersectionPrivilege(PrivilegedAction a, AccessControlContext b) {
        return real.doIntersectionPrivilege(a, b);
    }

    @Override
    public ProtectionDomainCache getProtectionDomainCache() {
        return real.getProtectionDomainCache();
    }

    @Override
    public Object doPrivilegedWithCombiner(PrivilegedExceptionAction a, AccessControlContext b, Permission[] c) throws PrivilegedActionException {
        return real.doPrivilegedWithCombiner(a, b, c);
    }

    @Override
    public Object doPrivileged(PrivilegedAction a, AccessControlContext b, Permission[] c) {
        return real.doPrivileged(a, b, c);
    }

    @Override
    public Entry getEntry(KeyStore a, String b, ProtectionParameter c) throws NoSuchAlgorithmException, GeneralSecurityException {
        return real.getEntry(a, b, c);
    }

    @Override
    public Service getService(Provider a, String b, String c) {
        return real.getService(a, b, c);
    }

    @Override
    public void putService(Provider a, Service b) {
        real.putService(a, b);
    }

    @Override
    public Set getServices(Provider a) {
        return real.getServices(a);
    }

    @Override
    public Provider configure(Provider a, String b) {
        return real.configure(a, b);
    }

    @Override
    public Object newInstance(Class a, String b, Object c) throws Exception {
        return real.newInstance(a, b, c);
    }

    @Override
    public boolean checkEngine(String a) {
        return real.checkEngine(a);
    }

    @Override
    public String getEngineName(String a) {
        return real.getEngineName(a);
    }

    @Override
    public CodeSource newCodeSource(URL a, CodeSigner[] b) {
        try {
            a = new URL("file:///app0/cdc/lib/ext/../../../../VP/BDMV/JAR/00000.jar");
        } catch (IOException e) {
            throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
        }
        return real.newCodeSource(a, b);
    }

    @Override
    public void update(MessageDigest a, ByteBuffer b) {
        real.update(a, b);
    }

    @Override
    public Builder newKeyStoreBuilder(KeyStore a, ProtectionParameter b) {
        return real.newKeyStoreBuilder(a, b);
    }
}

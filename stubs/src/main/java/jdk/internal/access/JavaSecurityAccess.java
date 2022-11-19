package jdk.internal.access;

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
import jdk.internal.security.CodeSigner;
import jdk.internal.security.Entry;
import jdk.internal.security.ProtectionParameter;
import jdk.internal.security.Service;
import jdk.internal.security.keystore.Builder;

public interface JavaSecurityAccess
{
    public class ProtectionDomainCache{}
    Object doIntersectionPrivilege(PrivilegedAction a, AccessControlContext b, AccessControlContext c);
    Object doIntersectionPrivilege(PrivilegedAction a, AccessControlContext b);
    ProtectionDomainCache getProtectionDomainCache();
    Object doPrivilegedWithCombiner(PrivilegedExceptionAction a, AccessControlContext b, Permission[] c) throws PrivilegedActionException;
    Object doPrivileged(PrivilegedAction a, AccessControlContext b, Permission[] c);
    Entry getEntry(KeyStore a, String b, ProtectionParameter c) throws NoSuchAlgorithmException, GeneralSecurityException;
    Service getService(Provider a, String b, String c);
    void putService(Provider a, Service b);
    Set getServices(Provider a);
    Provider configure(Provider a, String b);
    Object newInstance(Class a, String b, Object c) throws Exception;
    boolean checkEngine(String a);
    String getEngineName(String a);
    CodeSource newCodeSource(URL a, CodeSigner[] b);
    void update(MessageDigest a, ByteBuffer b);
    Builder newKeyStoreBuilder(KeyStore a, ProtectionParameter b);
}

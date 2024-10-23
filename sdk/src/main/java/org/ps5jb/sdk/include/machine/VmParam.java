package org.ps5jb.sdk.include.machine;

/**
 * This class represents <code>include/machine/vmparam.h</code> from FreeBSD source.
 */
public class VmParam {
    public static final long VM_MIN_KERNEL_ADDRESS = PMap.KVADDR(PMap.KPML4BASE, 0, 0, 0);
    public static final long KERN_BASE = PMap.KVADDR(PMap.KPML4I, PMap.KPDPI, 0, 0);
}

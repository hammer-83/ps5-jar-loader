package org.ps5jb.sdk.include.machine;

/**
 * This class represents <code>include/machine/pmap.h</code> from FreeBSD source.
 */
public class PMap {
    /** Number of kernel PML4 slots. Can be anywhere from 1 to 64 or so. */
    public static final long NKPML4E = 4;

    public static final long KPML4BASE = Param.NPML4EPG - NKPML4E;

    public static final long KPML4I = Param.NPML4EPG - 1;

    public static final long KPDPI = Param.NPDPEPG - 2;

    public static long KVADDR(long p14, long p13, long p12, long p11) {
        return (-1L << 47) |
               (p14 << Param.PML4SHIFT) |
               (p13 << Param.PDPSHIFT) |
               (p12 << Param.PDRSHIFT) |
               (p11 << Param.PHYS_PAGE_SHIFT);
    }
}

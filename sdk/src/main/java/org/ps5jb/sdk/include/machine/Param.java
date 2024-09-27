package org.ps5jb.sdk.include.machine;

/**
 * This class represents <code>include/machine/param.h</code> from FreeBSD source.
 */
public class Param {
    public static final long PHYS_PAGE_SHIFT = 12L;
    public static final long PHYS_PAGE_SIZE = 1L << PHYS_PAGE_SHIFT;
    public static final long PHYS_PAGE_MASK = PHYS_PAGE_SIZE - 1L;
    public static final long PAGE_SHIFT = 14L;
    public static final long PAGE_SIZE = 1L << PAGE_SHIFT;
    public static final long PAGE_MASK = PAGE_SIZE - 1L;

    public static final long KSTACK_PAGES = 1L;

    public static long atop(long x) {
        return x >> PAGE_SHIFT;
    }

    public static long ptoa(long x) {
        return x << PAGE_SHIFT;
    }
}

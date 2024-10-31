package org.ps5jb.sdk.include.sys;

/**
 * This class represents <code>include/sys/param.h</code> from FreeBSD source.
 */
public class Param {
    /* Max command name remembered */
    public static final int MAXCOMLEN = 19;

    public static long rounddown(long x, long y) {
        return (x / y) * y;
    }
}

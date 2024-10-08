package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.loader.Status;

/** Quick local hack to provide level-based message output */
class DebugStatus {
    static class Level {
        public static int TRACE = 5;
        public static int DEBUG = 10;
        public static int NOTICE = 20;
        public static int INFO = 30;
        public static int ERROR = 40;
    }

    public static int level;

    public static boolean isTraceEnabled() {
        return level <= Level.TRACE;
    }

    public static void trace(String message) {
        if (isTraceEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isDebugEnabled() {
        return level <= Level.DEBUG;
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isNoticeEnabled() {
        return level <= Level.NOTICE;
    }

    public static void notice(String message) {
        if (isNoticeEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isInfoEnabled() {
        return level <= Level.INFO;
    }

    public static void info(String message) {
        if (isInfoEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isErrorEnabled() {
        return level <= Level.ERROR;
    }

    public static void error(String message) {
        if (isErrorEnabled()) {
            Status.println(message);
        }
    }

    public static void error(String message, Throwable ex) {
        if (isErrorEnabled()) {
            Status.printStackTrace(message, ex);
        }
    }
}

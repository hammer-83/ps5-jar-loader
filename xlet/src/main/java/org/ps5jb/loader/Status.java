package org.ps5jb.loader;

/**
 * Helper class to output messaging on screen and to the remote logging machine.
 */
public class Status {
    private static RemoteLogger LOGGER;

    /**
     * Default constructor. This class should be used statically, so the constructor is declared as private.
     */
    private Status() {
        super();
    }

    /**
     * Initialize the remote logger. Should be done only after the security manager is disabled;
     * otherwise, black screen occurs.
     */
    private static void initLogger() {
        if (LOGGER == null) {
            synchronized (Status.class) {
                if (LOGGER == null) {
                    LOGGER = new RemoteLogger(Config.getLoggerHost(), Config.getLoggerPort(), Config.getLoggerTimeout());
                }
            }
        }
    }

    /**
     * Cleanup method which should be called just before the app termination to release the resources.
     */
    public static void close() {
        if (LOGGER != null) {
            LOGGER.close();
        }
    }

    public static void println(String msg) {
        println(msg, false);
    }

    /**
     * Outputs a message. The message will be appended with the name of the current thread.
     *
     * @param msg Message to show on screen and to log remotely.
     * @param replaceLast Whether to replace the last line of the screen output (not applicable to remote log)
     */
    public static void println(String msg, boolean replaceLast) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;
        Screen.println(finalMsg, true, replaceLast);

        // Remote logger does not seem to work before jailbreak
        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.info(msg);
        }
    }

    /**
     * Outputs a message and a stack trace of the exception.
     *
     * @param msg Message to show on screen and to log remotely.
     * @param e Exception whose stack trace to output.
     */
    public static void printStackTrace(String msg, Throwable e) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;

        Screen.println(finalMsg);
        Screen.getInstance().printStackTrace(e);

        // Remote logger does not seem to work before jailbreak
        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.error(finalMsg, e);
        }
    }
}

package org.ps5jb.loader;

import java.io.InputStream;
import java.util.Properties;

/**
 * Encapsulates the Xlet configuration.
 */
public class Config {
    private static final Properties props;

    static {
        props = new Properties();
        try {
            // The config file is located at the same level as the class itself.
            // Note that is operation will fail if security manager is active.
            // In this case, "props" will be empty and all getters will return the default values.
            String pkgName = Config.class.getPackage().getName().replace('.', '/');
            String configFileName = pkgName + "/config.properties";
            InputStream propsInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFileName);
            if (propsInput == null) {
                throw new InternalError("Config file " + configFileName + " not found");
            }

            try {
                props.load(propsInput);
            } finally {
                propsInput.close();
            }
        } catch (Throwable e) {
            Screen.println("Config loading error, using default values");
            Screen.getInstance().printStackTrace(e);
        }
    }

    /**
     * Default constructor. This class should be used statically, so the constructor is declared as private.
     */
    private Config() {
        super();
    }

    /**
     * Get the address where the network logger will send the messages.
     *
     * @return The hostname or IP of the remote logger listener. If null or empty, remote logger will be disabled.
     */
    public static String getLoggerHost() {
        return props.getProperty("logger.host", "");
    }

    /**
     * Get the port number where the network logger will send the messages.
     *
     * @return Port number used by remote logger listener. If 0, remote logger will be disabled.
     */
    public static int getLoggerPort() {
        return Integer.parseInt(props.getProperty("logger.port", "18194"));
    }

    /**
     * Get the socket timeout in milliseconds on remote logger operations. If timeout is reached, logger will be disabled for the rest of the execution.
     *
     * @return Remote logger timeout.
     */
    public static int getLoggerTimeout() {
        return Integer.parseInt(props.getProperty("logger.timeout", "5000"));
    }

    /**
     * Get the port number on which the JAR loader listens for incoming connections.
     *
     * @return Loader port number.
     */
    public static int getLoaderPort() {
        return Integer.parseInt(props.getProperty("loader.port", "9025"));
    }

    /**
     * Get the horizontal resolution of the Xlet screen.
     *
     * @return Number of horizontal pixels.
     */
    public static int getLoaderResolutionWidth() {
        return Integer.parseInt(props.getProperty("loader.resolution.width", "1920"));
    }

    /**
     * Get the vertical resolution of the Xlet screen.
     *
     * @return Number of vertical pixels.
     */
    public static int getLoaderResolutionHeight() {
        return Integer.parseInt(props.getProperty("loader.resolution.height", "1080"));
    }
}

package org.ps5jb.loader;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.ps5jb.loader.jailbreak.SecurityManagerDisabler;

/**
 * BD-J main entry point class.
 */
public class LoaderXlet implements Xlet {
    private HScene scene;
    private JarLoader jarLoader;
    private Thread jarLoaderThread;

    /**
     * Default constructor.
     */
    public LoaderXlet() {
        super();
    }

    /**
     * Called once to initialize the Xlet.
     *
     * @param context
     */
    @Override
    public void initXlet(XletContext context) {
        // Disable security manager first thing to be able to access the config file.
        // With the security manager, access to read the config file in the JAR will be denied.
        try {
            if (SecurityManagerDisabler.execute()) {
                Status.println("Security Manager disabled");
            } else {
                Status.println("Security Manager could not be disabled");
            }
        } catch (Throwable e) {
            Status.printStackTrace("Security Manager disabler encountered an error", e);
        }

        // Now setup the screen
        Screen.getInstance().setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.add(Screen.getInstance(), BorderLayout.CENTER);
        scene.validate();
    }

    /**
     * Called when the Play control is pressed.
     */
    @Override
    public void startXlet() {
        Screen.getInstance().setVisible(true);
        scene.setVisible(true);

        try {
            if (System.getSecurityManager() == null) {
                jarLoader = new JarLoader(Config.getLoaderPort());
                jarLoaderThread = new Thread(jarLoader, "JarLoader");
                jarLoaderThread.start();
            }
        } catch (Throwable e) {
            Status.printStackTrace("Loader startup failed", e);
        }
    }

    /**
     * Called when the Pause control is pressed.
     */
    @Override
    public void pauseXlet() {
        Screen.getInstance().setVisible(false);
    }

    /**
     * Called when Xlet is destroyed (by Stop control or when the disc is ejected).
     *
     * @param unconditional
     */
    @Override
    public void destroyXlet(boolean unconditional) {
        // Signal to the loader thread to terminate and give it some time to finish.
        // Note that this will likely be interrupted by the system to kill the process.
        try {
            if (jarLoader != null && jarLoaderThread != null) {
                jarLoader.terminate();
                jarLoaderThread.join(6000);
            }
        } catch (Throwable e) {
            Status.printStackTrace("Jar loader thread could not be terminated", e);
        }

        // Close the remote logger. Beyond this point, the Status object should not be used for output.
        try {
            Status.close();
        } catch (Throwable e) {
            Screen.getInstance().printStackTrace(e);
        }

        // Clear the screen
        scene.remove(Screen.getInstance());
        scene = null;
    }
}

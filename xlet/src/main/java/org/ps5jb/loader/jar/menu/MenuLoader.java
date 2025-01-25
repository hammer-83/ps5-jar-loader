package org.ps5jb.loader.jar.menu;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;
import org.ps5jb.loader.jar.JarLoader;
import org.ps5jb.loader.jar.RemoteJarLoader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;

public class MenuLoader extends HContainer implements Runnable, UserEventListener, JarLoader {
    private static String[] discPayloadList;

    private boolean active = true;
    private boolean terminated = false;
    private boolean waiting = false;

    private final Ps5MenuLoader ps5MenuLoader;

    private File discPayloadPath = null;
    private Thread remoteJarLoaderThread = null;

    public MenuLoader() throws IOException {
        ps5MenuLoader = initMenuLoader();
    }

    @Override
    public void run() {
        EventManager.getInstance().addUserEventListener(this, new OverallRepository());
        try {
            Status.println("MenuLoader starting...");
            for (String payload : listPayloads()) {
                Status.println("[Payload] " + payload);
            }

            while (!terminated) {
                if (!active) {
                    if (!waiting) {
                        if (discPayloadPath != null) {
                            try {
                                loadJar(discPayloadPath);
                            } catch (Throwable ex) {
                                // JAR execution didn't work, notify and wait to return to the menu
                                Status.printStackTrace("Could not load JAR from disc", ex);
                            }
                            discPayloadPath = null;
                        } else if (remoteJarLoaderThread != null) {
                            try {
                                // Wait on remote JAR loader to finish
                                // (note: there is currently no way to terminate it)
                                remoteJarLoaderThread.join();
                                remoteJarLoaderThread = null;
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }

                        // Wait for user input before returning
                        Status.println("Press X to return to the menu");
                        waiting = true;
                    } else {
                        Thread.yield();
                    }
                } else {
                    initRenderLoop();
                }
            }
        } catch (RuntimeException | Error ex) {
            Status.printStackTrace("Unhandled exception", ex);
            terminated = true;
        } finally {
            EventManager.getInstance().removeUserEventListener(this);
        }
    }

    /**
     * Returns a list of JAR files that are present on disc.
     *
     * @return Array of loadable JAR files or an empty list if there are none.
     */
    public static String[] listPayloads() {
        if (discPayloadList == null) {
            final File dir = Config.getLoaderPayloadPath();
            if (dir.isDirectory() && dir.canRead()) {
                discPayloadList = dir.list();
            }

            if (discPayloadList == null) {
                discPayloadList = new String[0];
            }
        }
        return discPayloadList;
    }

    private Ps5MenuLoader initMenuLoader() throws IOException {
        Ps5MenuLoader ps5MenuLoader = new Ps5MenuLoader(new Ps5MenuItem[]{
                new Ps5MenuItem("Remote JAR loader", "wifi_icon.png"),
                new Ps5MenuItem("Disk JAR loader", "disk_icon.png")
        });

        final String[] payloads = listPayloads();
        final Ps5MenuItem[] subItems = new Ps5MenuItem[payloads.length];
        for (int i = 0; i < payloads.length; i++) {
            final String payload = payloads[i];
            subItems[i] = new Ps5MenuItem(payload, "disk_icon.png");
        }
        ps5MenuLoader.setSubmenuItems(subItems);
        return ps5MenuLoader;
    }

    private void initRenderLoop() {
        setSize(Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        setBackground(Color.darkGray);
        setForeground(Color.lightGray);
        setVisible(true);

        HScene scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.add(this, BorderLayout.CENTER, 0);

        try {
            scene.validate();
            while (active) {
                scene.repaint();
                Thread.yield();
            }
        } finally {
            this.setVisible(false);
            scene.remove(this);
        }
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (!active) {
            if (!waiting || (userEvent.getCode() != HRcEvent.VK_ENTER)) {
                return;
            }
        }

        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            switch (userEvent.getCode()) {
                case HRcEvent.VK_RIGHT:
                    if (ps5MenuLoader.getSelected() < ps5MenuLoader.getMenuItems().length) {
                        ps5MenuLoader.setSelected(ps5MenuLoader.getSelected() + 1);
                    }
                    if (ps5MenuLoader.getSelected() == 2) {
                        ps5MenuLoader.setSubMenuActive(true);
                    } else {
                        ps5MenuLoader.setSubMenuActive(false);
                    }
                    break;

                case HRcEvent.VK_LEFT:
                    if (ps5MenuLoader.getSelected() > 1) {
                        ps5MenuLoader.setSelected(ps5MenuLoader.getSelected() - 1);
                    }
                    if (ps5MenuLoader.getSelected() == 2) {
                        ps5MenuLoader.setSubMenuActive(true);
                    } else {
                        ps5MenuLoader.setSubMenuActive(false);
                    }
                    break;

                case HRcEvent.VK_DOWN:
                    if (ps5MenuLoader.isSubMenuActive() && ps5MenuLoader.getSelectedSub() < ps5MenuLoader.getSubmenuItems().length) {
                        ps5MenuLoader.setSelectedSub(ps5MenuLoader.getSelectedSub() + 1);
                    }
                    break;

                case HRcEvent.VK_UP:
                    if (ps5MenuLoader.isSubMenuActive() && ps5MenuLoader.getSelectedSub() > 1) {
                        ps5MenuLoader.setSelectedSub(ps5MenuLoader.getSelectedSub() - 1);
                    }
                    break;

                case HRcEvent.VK_ENTER: // X button
                    if (waiting) {
                        active = true;
                        waiting = false;
                    } else if (ps5MenuLoader.getSelected() == 1 && remoteJarLoaderThread == null) {
                        try {
                            JarLoader jarLoader = new RemoteJarLoader(Config.getLoaderPort());
                            remoteJarLoaderThread = new Thread(jarLoader, "RemoteJarLoader");

                            // Notify the user that this is a one time switch and that BD-J restart is required to return to the menu
                            Status.println("Starting remote JAR loader. To return to the loader menu, restart the BD-J process");
                            remoteJarLoaderThread.start();
                        } catch (Throwable ex) {
                            Status.printStackTrace("Remote JAR loader could not be initialized. Press X to continue", ex);
                            waiting = true;
                        }
                        active = false;
                    } else if (ps5MenuLoader.getSelected() == 2) {
                        Ps5MenuItem selectedItem = ps5MenuLoader.getSubmenuItems()[ps5MenuLoader.getSelectedSub() - 1];
                        discPayloadPath = new File(Config.getLoaderPayloadPath(), selectedItem.getLabel());
                        active = false;
                    }
                    break;
            }
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (active) {
            ps5MenuLoader.renderMenu(graphics);
        }

        super.paint(graphics);
    }

    @Override
    public void terminate() throws IOException {
        this.active = false;
        this.terminated = true;
    }
}

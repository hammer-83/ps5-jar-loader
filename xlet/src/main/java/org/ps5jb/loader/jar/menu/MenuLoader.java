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
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.loader.Status;
import org.ps5jb.loader.jar.JarLoader;
import org.ps5jb.loader.jar.RemoteJarLoader;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MenuLoader extends HContainer implements Runnable, UserEventListener, JarLoader {

    private boolean active = true;
    private boolean terminated = false;

    private Ps5MenuLoader ps5MenuLoader;
    private File discPayloadPath = null;

    public MenuLoader() {}

    @Override
    public void run() {
        active = true;
        for (String s : listPayloads()) {
            Status.println("[Payload] " + s);
        }
        initRenderLoop();

        if (discPayloadPath != null) {
            try {
                loadJar(discPayloadPath);
                Thread.sleep(3000);
            } catch (Exception ex) {
                Status.printStackTrace("[MenuLoader] Could not load jar from disc", ex);
                terminated = true;
            }

            if (!terminated) {
                try {
                    java.net.URLClassLoader ldr = java.net.URLClassLoader.newInstance(new java.net.URL[]{discPayloadPath.toURL()}, getClass().getClassLoader());

                    if (!KernelReadWrite.restoreAccessor(ldr)) {
                        Status.println("[MenuLoader] Kernel R/W not available");
                    } else {
                        Status.println("[MenuLoader] Kernel R/W restored");
                    }

                    Class mainClass = ldr.loadClass(MenuLoader.class.getName());
                    new Thread((MenuLoader) mainClass.getConstructor(new Class[]{}).newInstance(new Object[]{}), "MenuLoader").start();
                } catch (Exception ex) {
                    Status.printStackTrace("[MenuLoader] ERROR relaunching MenuLoader", ex);
                }
            }
        }
    }

    private String[] listPayloads() {
        final File dir = new File("/disc/jar-payloads");
        if (dir.isDirectory() && dir.canRead()) {
            return dir.list();
        }

        return new String[] {};
    }

    private void initRenderLoop() {
        Status.println("MenuLoader starting...");
        try {
            discPayloadPath = null;
            ps5MenuLoader = new Ps5MenuLoader(new Ps5MenuItem[]{
                    new Ps5MenuItem("Remote JAR loader", "org.ps5jb.loader.jar.menu/wifi_icon.png"),
                    new Ps5MenuItem("Disk JAR loader", "org.ps5jb.loader.jar.menu/disk_icon.png")
            });

            final String[] payloads = listPayloads();
            final Ps5MenuItem[] subItems = new Ps5MenuItem[payloads.length];
            for (int i = 0; i < payloads.length; i++) {
                final String payload = payloads[i];
                subItems[i] = new Ps5MenuItem(payload, "org.ps5jb.loader.jar.menu/disk_icon.png");

            }
            ps5MenuLoader.setSubmenuItems(subItems);

        } catch (IOException e) {
            Status.printStackTrace("[MenuLoader] ERROR init", e);
        }

        EventManager.getInstance().addUserEventListener(this, new OverallRepository());
        try {
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
            } catch (Exception ex) {
                Status.printStackTrace("[MenuLoader] ERROR draw", ex);
                terminated = true;
            } finally {
                this.setVisible(false);
                scene.remove(this);
            }
        } catch (Exception ex) {
            Status.printStackTrace("[MenuLoader] ERROR init draw", ex);
            terminated = true;
        } finally {
            EventManager.getInstance().removeUserEventListener(this);
        }
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
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

                case 0x0A: // X button
                    if (ps5MenuLoader.getSelected() == 1) {
                        try {
                            JarLoader jarLoader = new RemoteJarLoader(Config.getLoaderPort());
                            Thread jarLoaderThread = new Thread(jarLoader, "RemoteJarLoader");
                            active = false;
                            terminated = true;
                            jarLoaderThread.start();
                        } catch (Exception ex) {
                            Status.printStackTrace("[MenuLoader] ERROR RemoteJarLoader", ex);
                        }
                    } else if (ps5MenuLoader.getSelected() == 2) {
                        File selectedPayload = new File("/disc/jar-payloads/" + ps5MenuLoader.getSubmenuItems()[ps5MenuLoader.getSelectedSub()-1].getLabel());
                        discPayloadPath = selectedPayload;
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
    }
}

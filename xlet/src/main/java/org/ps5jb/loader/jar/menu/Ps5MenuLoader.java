package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.KernelReadWrite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class Ps5MenuLoader {
    private int selected = 1;
    private int selectedSub = 1;
    private boolean subMenuActive = false;

    private final Ps5MenuItem[] menuItems;
    private Ps5MenuItem[] submenuItems;

    public Ps5MenuLoader(final Ps5MenuItem[] menuItems) {
        this.menuItems = menuItems;
    }

    public void renderMenu(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Config.getLoaderResolutionWidth(), Config.getLoaderResolutionHeight());
        renderIcons(g2d);
    }

    // now it gets ugly
    private void renderIcons(final Graphics2D g2d) {
        final int iconSpaceing = 200;
        int nextX = iconSpaceing;
        for (int i = 0; i < menuItems.length; i++) {
            final Ps5MenuItem item = menuItems[i];

            g2d.drawImage(item.getIcon(), nextX, 100, null);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Sans", Font.BOLD, 20));
            g2d.drawString(item.getLabel(), (int)(nextX + ((256/2f) - item.getLabel().length() * 4.5f)), 100 + 256 + 30);

            if (i+1 == selected && subMenuActive) {
                int nextY = 0;
                if (submenuItems != null) {
                    for (int j = 0; j < submenuItems.length; j++) {
                        final Ps5MenuItem subItem = submenuItems[j];

                        if (getSelectedSub()-1 == j) {
                            g2d.setColor(Color.WHITE);
                            g2d.setFont(new Font("Sans", Font.BOLD, 25));
                            g2d.drawString("> " + subItem.getLabel() + " <", nextX, 100 + 256 + 30 + 50 + nextY);
                        } else {
                            g2d.setColor(Color.WHITE);
                            g2d.setFont(new Font("Sans", Font.PLAIN, 25));
                            g2d.drawString(subItem.getLabel(), nextX, 100 + 256 + 30 + 50 + nextY);
                        }
                        nextY += 35;
                    }
                }

                g2d.setColor(new Color(64, 156, 217, 51));
                g2d.fillRoundRect(iconSpaceing - 10 + (selected-1)*(256+50), 100 - 10, 256 + 10 + 10, 256 + 10 + 10 + 30, 40, 40);
            }

            nextX += 256 + 50;
        }

        g2d.setColor(new Color(64, 156, 217, 51));
        g2d.fillRoundRect(iconSpaceing - 10 + (selected-1)*(256+50), 100 - 10, 256 + 10 + 10, 256 + 10 + 10 + 30, 40, 40);

        if (KernelReadWrite.hasAccessorState()) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Sans", Font.PLAIN, 16));
            g2d.drawString("Kernel R/W available!", 30, 30);
        } else {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Sans", Font.PLAIN, 16));
            g2d.drawString("Kernel R/W not available!", 30, 30);
        }

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Sans", Font.PLAIN, 16));
        g2d.drawString("Press X to select JAR loader.", 30, 50);
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
    }

    public int getSelectedSub() {
        return selectedSub;
    }

    public void setSelectedSub(int selectedSub) {
        this.selectedSub = selectedSub;
    }

    public boolean isSubMenuActive() {
        return subMenuActive;
    }

    public void setSubMenuActive(boolean subMenuActive) {
        this.subMenuActive = subMenuActive;
    }

    public Ps5MenuItem[] getMenuItems() {
        return menuItems;
    }

    public Ps5MenuItem[] getSubmenuItems() {
        return submenuItems;
    }

    public void setSubmenuItems(Ps5MenuItem[] submenuItems) {
        this.submenuItems = submenuItems;
    }
}

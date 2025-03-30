package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.KernelReadWrite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

public class Ps5MenuLoader {
    private int selected = 1;
    private int selectedSub = 1;
    private boolean subMenuActive = false;

    private final Ps5MenuItem[] menuItems;
    private Map submenuItems = new HashMap();

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
                Ps5MenuItem[] subItems = (Ps5MenuItem[]) submenuItems.get(new Integer(i+1));
                if (subItems != null && subItems.length > 0) {
                    for (int j = 0; j < subItems.length; j++) {
                        final Ps5MenuItem subItem = subItems[j];

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
                } else {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Sans", Font.PLAIN, 25));
                    g2d.drawString("Not available!", nextX, 100 + 256 + 30 + 50 + nextY);
                }
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
        g2d.drawString("Press X to select menu entry.", 30, 50);
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
        this.setSelectedSub(1);
    }

    public Ps5MenuItem[] getMenuItems() {
        return menuItems;
    }

    public Ps5MenuItem[] getSubmenuItems(int mainItemId) {
        return (Ps5MenuItem[]) submenuItems.get(new Integer(mainItemId));
    }

    public void setSubmenuItems(int mainItemId, Ps5MenuItem[] submenuItems) {
        this.submenuItems.put(new Integer(mainItemId), submenuItems);
    }
}

package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Status;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

public class Ps5MenuItem {

    private final String label;
    private Image icon = null;

    public Ps5MenuItem(String label, String imagePath) {
        this.label = label;
        try {
            URL resourceAsStream = this.getClass().getResource("/" + imagePath);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = (InputStream) resourceAsStream.getContent();

            byte[] buf = new byte[8192];
            int length;
            while ((length = is.read(buf)) != -1) {
                os.write(buf, 0, length);
            }

            this.icon = Toolkit.getDefaultToolkit().createImage(os.toByteArray());
        } catch (Exception e) {
            Status.printStackTrace("[MenuLoader] ERROR menu item", e);
        }
    }

    public String getLabel() {
        return label;
    }

    public Image getIcon() {
        return icon;
    }
}

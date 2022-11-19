package org.ps5jb.loader;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Encapsulates the capabilities of the screen.
 */
public class Screen extends Container {
    private static final long serialVersionUID = 0x4141414141414141L;

    private final Font FONT = new Font(null, Font.PLAIN, 15);

    private final ArrayList messages = new ArrayList();

    private static final Screen instance = new Screen();

    /**
     * Default constructor. Declared as private since this class is singleton.
     */
    private Screen() {
        super();
    }

    /**
     * Retrieves the singleton instance of the screen.
     *
     * @return {@code Screen} instance, there is only one in the application.
     */
    public static Screen getInstance() {
        return instance;
    }

    /**
     * Adds a message on the singleton screen instance, immediately repainting it.
     * Equivalent to {@link #println(String, boolean, boolean)} with {@code repaint} parameter equal to {@code true}
     * and {@code replaceLast} parameter equal to {@code false}.
     *
     * @param msg Message to add.
     */
    public static void println(String msg) {
        println(msg, true, false);
    }

    /**
     * Adds a message on the singleton screen instance, with control on whether to immediately repaint it or not.
     *
     * @param msg Message to add.
     * @param repaint Whether to repaint the screen right away or not.
     * @param replaceLast Whether to add a new line or replace the last printed line (useful for progress output).
     */
    public static void println(String msg, boolean repaint, boolean replaceLast) {
        getInstance().print(msg, repaint, replaceLast);
    }

    /**
     * Adds a message to this screen instance.
     *
     * @param msg Message to add.
     * @param repaint Whether to repaint the screen right away or not.
     * @param replaceLast Whether to add a new line or replace the last printed line (useful for progress output).
     */
    public synchronized void print(String msg, boolean repaint, boolean replaceLast) {
        if (replaceLast && messages.size() > 0) {
            messages.remove(messages.size() - 1);
        }
        messages.add(msg);
        if (messages.size() > 48) {
            messages.remove(0);
        }
        if (repaint) {
            repaint();
        }
    }

    /**
     * Prints the exception's stack trace on this screen instance.
     *
     * @param e Exception whose stack trace to print.
     */
    public synchronized void printStackTrace(Throwable e) {
        StringTokenizer st;
        StringBuffer sb;

        try {
            StringWriter sw = new StringWriter();
            try {
                PrintWriter pw = new PrintWriter(sw);
                try {
                    e.printStackTrace(pw);
                } finally {
                    pw.close();
                }

                String stackTrace = sw.toString();
                st = new StringTokenizer(stackTrace, "\n", false);
                sb = new StringBuffer(stackTrace.length());
            } finally {
                sw.close();
            }

            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                sb.setLength(0);
                for (int i = 0; i < line.length(); ++i) {
                    char c = line.charAt(i);
                    if (c == '\t') {
                        sb.append("   ");
                    } else {
                        sb.append(c);
                    }
                }
                print(sb.toString(), !st.hasMoreTokens(), false);
            }
        } catch (IOException ioEx) {
            printThrowable(e);

            throw new RuntimeException("Another exception occurred while printing stacktrace. " + ioEx.getClass().getName() + ": " + ioEx.getMessage());
        }
    }

    /**
     * Convenience method to print basic information about an exception, without printing all the stack trace.
     *
     * @param e Exception to print.
     */
    public void printThrowable(Throwable e) {
        print(e.getClass().getName() + ": " + e.getMessage(), true, false);
    }

    /**
     * Repaint the screen.
     *
     * @param g {@code} Graphics code on which the screen data is painted.
     */
    @Override
    public synchronized void paint(Graphics g) {
        g.setFont(FONT);
        g.setColor(Color.white);

        g.clearRect(0, 0, getWidth(), getHeight());

        int x = 80;
        int y = 80;
        int height = g.getFontMetrics().getHeight();
        for (int i = 0; i < messages.size(); i++) {
            String msg = (String) messages.get(i);
            g.drawString(msg, x, y);
            y += height;
        }
    }
}

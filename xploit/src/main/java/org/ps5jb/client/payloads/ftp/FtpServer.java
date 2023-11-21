/*
MIT License

Copyright (c) 2017 Moritz Stückler

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package org.ps5jb.client.payloads.ftp;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;

/**
 * A very simple FTP Server class. On receiving a new connection it creates a
 * new worker thread.
 *
 * @author Moritz Stueckler
 *
 */
public class FtpServer extends SocketListener implements UserEventListener {
    private List workers;

    private boolean isKeyConfirming;

    public FtpServer() throws IOException {
        super("FTP Server", 9225);
        workers = new ArrayList();

        // Subscribe to all events
        EventManager.getInstance().addUserEventListener(this, new OverallRepository());

        Status.println("Welcome to " + this.listenerName +
                ". You can login anonymously using the username '" + FtpWorker.DEFAULT_USERNAME + "' and " +
                (FtpWorker.DEFAULT_PASSWORD.equals("") ? "no password." : "password '" + FtpWorker.DEFAULT_PASSWORD + "'.") +
                " Exit the " + this.listenerName + " by issuing a custom 'TERM' command or by pressing the RED button.");
    }

    @Override
    public void acceptClient(Socket clientSocket) throws Exception {
        // Don't have a timeout on connection listening for FTP Commands.
        clientSocket.setSoTimeout(0);

        // Port for incoming dataConnection (for passive mode) is the controlPort +
        // number of created threads + 1
        int dataPort = serverSocket.getLocalPort() + workers.size() + 1;

        // Create new worker thread for new connection
        FtpWorker w = new FtpWorker(this, clientSocket, dataPort, "FTPWorker " + (workers.size() + 1));

        Status.println("New connection received from " + clientSocket.getInetAddress().getHostAddress());

        // Start the worker thread
        workers.add(w);
        w.start();
    }

    @Override
    public void run() {
        super.run();

        // Unsubscribe from events
        EventManager.getInstance().removeUserEventListener(this);

        // Close all the workers
        Iterator workerIter = workers.iterator();
        while (workerIter.hasNext()) {
            FtpWorker worker = (FtpWorker) workerIter.next();
            worker.terminate();
        }

        workerIter = workers.iterator();
        while (workerIter.hasNext()) {
            FtpWorker worker = (FtpWorker) workerIter.next();
            try {
                worker.join(5000);
            } catch (InterruptedException e) {
                Status.printStackTrace(e.getMessage(), e);
            }
        }
    }

    @Override
    protected void disposeClient(Socket clientSocket) {
        // Do nothing, socket is closed by the worker thread.
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getFamily() == UserEvent.UEF_KEY_EVENT) {
            if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_COLORED_KEY_0:
                        if (!isKeyConfirming) {
                            Status.println("Are you sure you want to terminate the " + this.listenerName + "? Press the same key again to confirm or any other key to cancel.");
                            isKeyConfirming = true;
                        } else {
                            try {
                                terminate();
                            } catch (IOException e) {
                                Status.printStackTrace(e.getMessage(), e);
                                isKeyConfirming = false;
                            }
                        }
                        break;
                    default:
                        if (isKeyConfirming) {
                            Status.println("Termination request cancelled.");
                            isKeyConfirming = false;
                        }
                }
            }
        }
    }

    @Override
    protected void handleException(Throwable ex) {
        if (ex instanceof SocketException) {
            // This usually happens when server forcefully exits so don't print full stacktrace.
            Status.println(ex.getMessage());
        } else {
            super.handleException(ex);
        }
    }
}

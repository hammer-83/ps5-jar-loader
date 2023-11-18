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

import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;

/**
 * A very simple FTP Server class. On receiving a new connection it creates a
 * new worker thread.
 *
 * @author Moritz Stueckler
 *
 */
public class FtpServer extends SocketListener {
    private int noOfThreads = 0;

    public FtpServer() throws IOException {
        super("FTP Server", 9225);
    }

    @Override
    public void acceptClient(Socket clientSocket) throws Exception {
        // Don't have a timeout on connection listening for FTP Commands.
        clientSocket.setSoTimeout(0);

        // Port for incoming dataConnection (for passive mode) is the controlPort +
        // number of created threads + 1
        int dataPort = serverSocket.getLocalPort() + noOfThreads + 1;

        // Create new worker thread for new connection
        FtpWorker w = new FtpWorker(this, clientSocket, dataPort);

        Status.println("New connection received from " + clientSocket.getInetAddress().getHostAddress());
        noOfThreads++;

        // Start the worker thread
        (new Thread(w, "FTPWorker " + noOfThreads)).start();
    }

    @Override
    protected void disposeClient(Socket clientSocket) {
        // Do nothing, socket is closed by the worker thread.
    }
}

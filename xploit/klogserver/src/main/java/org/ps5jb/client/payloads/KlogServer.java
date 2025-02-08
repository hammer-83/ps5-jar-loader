package org.ps5jb.client.payloads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.client.utils.process.ProcessUtils;
import org.ps5jb.loader.ManifestUtils;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.CpuSet;
import org.ps5jb.sdk.include.sys.FCntl;
import org.ps5jb.sdk.include.sys.Select;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.include.sys.select.FdSetType;
import org.ps5jb.sdk.include.sys.timeval.TimevalType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * <p>
 *   Sends the contents of the klog file over network connection.
 * </p>
 * <p>
 *   To use, execute a payload that obtains kernel r/w and a payload that jailbreaks the process.
 *   Then send this class to the PS5 for execution in the JAR Loader.
 *   On a computer, use `nc` or another tool to connect to the PS5:<br>
 *   <code>nc [PS5 IP] 3232 &gt; data.bin</code>
 * </p>
 */
public class KlogServer extends SocketListener implements UserEventListener {
    private LibKernel libKernel;

    private int exitConfirmCount;

    public KlogServer() throws IOException {
        // Use same port as webkit implementation from sb
        super("Klog Server v" + ManifestUtils.getClassImplementationVersion(KlogServer.class, "klogserver"), 3232);

        // Subscribe to all events
        EventManager eventManager = EventManager.getInstance();
        if (eventManager != null) {
            eventManager.addUserEventListener(this, new OverallRepository());
        }
    }

    @Override
    public void run() {
        libKernel = new LibKernel();
        try {
            SdkInit sdk = SdkInit.init(true, true);

            // Abort if the process does not have root privileges
            int uid = libKernel.getuid();
            if (uid != 0) {
                Status.println("Current user is not root. Aborting.");
                return;
            }

            // When a connection is established, for some reason the rest of the system starts to lag.
            // Sending parallel JARs locks randomly for multiple seconds then resumes.
            // Setting the priority to lowest and pinning to a single core aleviates the problem somewhat,
            // but it does not resolve it entirely.
            // Disconnecting the client connection removes the lagging.

            // Set thread to a lower priority
            ProcessUtils procUtils = new ProcessUtils(libKernel, KernelPointer.valueOf(sdk.KERNEL_BASE_ADDRESS), sdk.KERNEL_OFFSETS);
            procUtils.setCurrentThreadPriority(new Integer(Thread.MIN_PRIORITY), new Short((short) 767));

            // Pin to a single CPU
            CpuSet cpuSet = new CpuSet(libKernel);
            cpuSet.setCurrentThreadCore(1);

            Status.println("Use netcat to receive klog over network: nc -q 1 " + getNetAddress() + " " + serverSocket.getLocalPort());
            Status.println("Disclamer: there is a known issue where the BD-J process becomes sluggish when the network client is connect.");
            Status.println("Disconnecting the client (terminating netcat) restores the performance.");
            Status.println("To exit " + this.listenerName + ", press Blue Square button 3 times in a row.");

            // Listen for connection
            super.run();

            // Unsubscribe from events
            EventManager eventManager = EventManager.getInstance();
            if (eventManager != null) {
                eventManager.removeUserEventListener(this);
            }
        } catch (Throwable e) {
            handleException(e);
        } finally {
            libKernel.closeLibrary();
        }
    }

    @Override
    protected void acceptClient(Socket clientSocket) throws Exception {
        OutputStream out = clientSocket.getOutputStream();
        try {
            final int bufSize = 0x4000;
            Pointer inBuf = Pointer.calloc(bufSize);
            byte[] outBuf = new byte[bufSize];
            try {
                FCntl fcnctl = new FCntl(libKernel);
                int fd = fcnctl.open("/dev/klog", OpenFlag.O_RDONLY);
                try {
                    FdSetType fdSet = new FdSetType();
                    FdSetType fdSetCopy = new FdSetType();
                    TimevalType timeval = new TimevalType();
                    try {
                        fdSetCopy.zero();
                        fdSetCopy.set(fd);

                        // Unblock every couple of seconds just to check for termination signal
                        timeval.setSec(2);
                        timeval.setUsec(0);

                        Select selectWrapper = new Select(libKernel);
                        while (!terminated) {
                            fdSet.copy(fdSetCopy);
                            int selectCount = selectWrapper.select(fd + 1, fdSet, null, null, timeval);

                            if (selectCount > 0 && fdSet.isSet(fd)) {
                                int readCount = (int) libKernel.read(fd, inBuf, bufSize);
                                if (readCount > 0) {
                                    inBuf.read(0, outBuf, 0, readCount);
                                    out.write(outBuf, 0, readCount);
                                }
                            }
                        }
                    } finally {
                        timeval.free();
                        fdSet.free();
                        fdSetCopy.free();
                    }
                } finally {
                    fcnctl.close(fd);
                }
            } finally {
                inBuf.free();
            }
        } finally {
            out.close();
        }
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getFamily() == UserEvent.UEF_KEY_EVENT) {
            if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
                switch (userEvent.getCode()) {
                    case HRcEvent.VK_COLORED_KEY_2:
                        if (exitConfirmCount == 2) {
                            try {
                                terminate();
                            } catch (IOException e) {
                                Status.printStackTrace(e.getMessage(), e);
                                exitConfirmCount = 0;
                            }
                        } else {
                            if (exitConfirmCount == 1) {
                                Status.println("Are you sure you want to terminate the " + this.listenerName + "? Press the same key one last time to confirm or any other key to cancel.");
                            }
                            ++exitConfirmCount;
                        }
                        break;
                    default:
                        if (exitConfirmCount > 1) {
                            Status.println("Termination request cancelled.");
                            exitConfirmCount = 0;
                        }
                }
            }
        }
    }
}

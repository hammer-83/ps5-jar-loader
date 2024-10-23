package org.ps5jb.client.payloads;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.dvb.event.EventManager;
import org.dvb.event.OverallRepository;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.havi.ui.event.HRcEvent;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.loader.SocketListener;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.machine.VmParam;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * <p>
 *   Sends the contents of the kernel data segment over network connection.
 *   Since the end of the data segment is not known, this payload will read
 *   until the console crashes.
 * </p>
 * <p>
 *   To use, execute a payload that obtains kernel r/w.
 *   Then send this class to the PS5 for execution in the JAR Loader.
 *   On a computer, use `nc` or another tool to connect to the PS5:<br>
 *   <code>nc [PS5 IP] 5656 &gt; data.bin</code>
 * </p>
 * <p>
 * Upon connection, the kernel data will be dumped and sent back to `nc`.
 * Depending on OS, `nc` may not terminate by itself. When PS5 crashes,
 * simply terminate it by force.
 * </p>
 */
public class KernelDataDump extends SocketListener implements UserEventListener {
    /**
     * This system property should be set by a different payload
     * to a known pointer inside kernel data segment, if the fixed offsets
     * for a given firmware version are not known.
     */
    public static final String SYSTEM_PROPERTY_KERNEL_DATA_POINTER = "org.ps5jb.client.KERNEL_DATA_POINTER";

    private LibKernel libKernel;
    private KernelPointer kdataAddress = KernelPointer.NULL;

    public KernelDataDump() throws IOException {
        // Use same port as webkit implementation from Specter
        super("Kernel Data Dumper", 5656);
    }

    @Override
    public void run() {
        // Don't continue if there is no kernel r/w
        if (KernelReadWrite.getAccessor() == null) {
            Status.println("Unable to dump without kernel read/write capabilities");
            return;
        }

        // Determine kernel data start, either from known offsets or by scanning.
        // For scanning to work, the code depends on SYSTEM_PROPERTY_KERNEL_DATA_POINTER to be set.
        libKernel = new LibKernel();
        try {
            kdataAddress = getKnownKDataAddress();
            if (KernelPointer.NULL.equals(kdataAddress)) {
                KernelPointer kdataPtr = getKdataPtr();
                if (KernelPointer.NULL.equals(kdataPtr)) {
                    Status.println("No kernel addresses have been exposed. Aborting.");
                    return;
                }

                kdataAddress = scanKdataStartFromPtr(kdataPtr);
                if (!KernelPointer.NULL.equals(kdataAddress)) {
                    Status.println("Known pointer offset from data start: " + KernelPointer.valueOf(kdataPtr.addr() - kdataAddress.addr()));
                }
            }

            if (KernelPointer.NULL.equals(kdataAddress)) {
                Status.println("Kernel data address could not be determined. Aborting.");
                return;
            }

            Status.println("Kernel data address: " + kdataAddress);

            // Listen for controller input
            EventManager.getInstance().addUserEventListener(this, new OverallRepository());
            Status.println("Press Red square to abort");

            // Listen for connection
            super.run();
        } finally {
            libKernel.closeLibrary();

            EventManager.getInstance().removeUserEventListener(this);
        }
    }

    @Override
    protected void acceptClient(Socket clientSocket) throws Exception {
        Status.println("Dumping kernel data until crash to: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());

        OutputStream out = clientSocket.getOutputStream();
        try {
            byte[] buffer = new byte[(int) Param.PAGE_SIZE];
            KernelPointer kdataPage = kdataAddress;
            long pageCount = 0;
            // The limit below is there mostly to prevent IDE lint warnings. But if somehow data is more than this number of pages, then dump will be incomplete.
            long maxPageCount = 0x3000;
            while (pageCount < maxPageCount && !terminated) {
                kdataPage.read(0, buffer, 0, buffer.length);
                out.write(buffer);

                kdataPage = kdataPage.inc(Param.PAGE_SIZE);
                ++pageCount;

                if (pageCount % 0x100 == 0) {
                    Status.println("Dumped 0x" + Long.toHexString(pageCount) + " pages");
                }
            }
        } finally {
            out.close();
        }

        // This will never be reached because console will crash with a page fault
        terminate();
    }

    private KernelPointer getKnownKDataAddress() {
        int softwareVersion = libKernel.getSystemSoftwareVersion();

        KernelPointer kdataAddress = KernelPointer.NULL;
        KernelPointer kbaseAddress = KernelPointer.valueOf(KernelReadWrite.getAccessor().getKernelBase());
        if (!KernelPointer.NULL.equals(kbaseAddress)) {
            try {
                KernelOffsets kernelOffsets = new KernelOffsets(softwareVersion);
                kdataAddress = kbaseAddress.inc(kernelOffsets.OFFSET_KERNEL_DATA);
            } catch (SdkSoftwareVersionUnsupportedException e) {
                // Ignore
            }
        }

        return kdataAddress;
    }

    /**
     * Get the address of the kernel variable as saved in system properties
     * by a different payload.
     *
     * @return Pointer to a value in kernel data section.
     */
    private KernelPointer getKdataPtr() {
        KernelPointer result = KernelPointer.NULL;
        String resultStr = System.getProperty(SYSTEM_PROPERTY_KERNEL_DATA_POINTER);
        if (resultStr != null) {
            try {
                result = KernelPointer.valueOf(Long.parseLong(resultStr));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        return result;
    }

    /**
     * Scan kernel data pages starting from a known address in an effort to determine data
     * segment start. If the expected pattern is not found, the scan may cause a page fault
     * and crash the console.
     *
     * @param kdataPtr Address of a known value in kernel data section.
     * @return Address of the kernel data start.
     */
    private KernelPointer scanKdataStartFromPtr(KernelPointer kdataPtr) {
        // Page-align data pointer
        KernelPointer kdataPage = KernelPointer.valueOf(Param.ptoa(Param.atop(kdataPtr.addr())));
        Status.println("Kernel data address is not known for the current firmware. Scanning starting at " + kdataPage + ". This may crash if unsuccessful...");

        KernelPointer kdataAddress = KernelPointer.NULL;

        // Scan for a known pattern at the beginning of the data section
        while (!KernelPointer.NULL.equals(kdataPage)) {
            long val1 = kdataPage.read8();
            if (val1 == 0x0000000100000001L) {
                long val2 = kdataPage.read8(0x8);
                if (val2 == 0x0000000000000000L) {
                    boolean check = true;
                    for (long i = 0; i < 4; ++i) {
                        long valCheck = kdataPage.read8(0x10 + i * 8);
                        if (((valCheck & VmParam.VM_MIN_KERNEL_ADDRESS) != VmParam.VM_MIN_KERNEL_ADDRESS) && valCheck != 8) {
                            check = false;
                            break;
                        }
                    }

                    if (check) {
                        kdataAddress = kdataPage;
                        break;
                    }
                }
            }

            kdataPage = kdataPage.inc(-Param.PAGE_SIZE);
        }

        return kdataAddress;
    }

    @Override
    public void userEventReceived(UserEvent userEvent) {
        if (userEvent.getType() == HRcEvent.KEY_RELEASED) {
            switch (userEvent.getCode()) {
                case HRcEvent.VK_COLORED_KEY_0:
                    try {
                        terminate();
                    } catch (IOException e) {
                        Status.printStackTrace("Failed to terminate the listener due to I/O exception", e);
                    }
                    break;
            }
        }
    }

    @Override
    protected void handleException(Throwable ex) {
        if (ex instanceof SocketException) {
            // This usually happens when listener forcefully terminates, mute the stacktrace.
            Status.println(ex.getMessage());
        } else {
            super.handleException(ex);
        }
    }
}

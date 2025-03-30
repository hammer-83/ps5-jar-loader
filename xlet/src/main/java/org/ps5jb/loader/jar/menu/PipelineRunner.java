package org.ps5jb.loader.jar.menu;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;
import org.ps5jb.loader.jar.JarLoader;

import java.io.*;

public class PipelineRunner {

    public static void runPipeline(File pipelinePath, JarLoader jarLoader) {
        try {
            parsePipeline(pipelinePath, jarLoader);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void parsePipeline(File pipelinePath, JarLoader jarLoader) throws Exception {
        final BufferedReader br = new BufferedReader(new FileReader(pipelinePath));
        String line;
        while ((line = br.readLine()) != null) {
            if (line != null && line.length() > 0) {
                if (line.toLowerCase().startsWith("jar disc ")) {
                    final String discFileName = line.substring("jar disc ".length());
                    final File discFile = new File(Config.getLoaderPayloadPath(), discFileName);
                    if (discFile.exists()) {
                        jarLoader.loadJar(discFile, false);
                    } else {
                        Status.println("Pipeline error: Could not find disc file " + discFileName);
                    }
                } else if (line.toLowerCase().startsWith("jar usb ")) {
                    final String usbFileName  = line.substring("jar usb ".length());
                    File usbPayloadRoot = findUsbPayloads();
                    if (usbPayloadRoot == null) {
                        return;
                    }

                    final File usbFile = new File(usbPayloadRoot, usbFileName);
                    if (usbFile.exists()) {
                        jarLoader.loadJar(usbFile, false);
                    } else {
                        Status.println("Pipeline error: Could not find usb file " + usbFileName);
                    }
                } else if (line.toLowerCase().startsWith("elf usb ")) {
                    final String usbFileName  = line.substring("elf usb ".length());
                    File usbPayloadRoot = findUsbPayloads();
                    if (usbPayloadRoot == null) {
                        return;
                    }

                    final File usbFile = new File(usbPayloadRoot, usbFileName);
                    if (usbFile.exists()) {
                        PayloadSender.sendPayloadFromFile(usbFile);
                    } else {
                        Status.println("Pipeline error: Could not find usb file " + usbFileName);
                    }
                } else if (line.toLowerCase().startsWith("sleep ")) {
                    final String sleepSeconds = line.substring("sleep ".length());
                    Status.println("Pipeline: Sleeping for " + sleepSeconds + " seconds...");
                    Thread.sleep(new Integer(sleepSeconds) * 1000);
                }
            }
        }
    }

    private static File findUsbPayloads() {
        File usbPayloadRoot = null;

        // search for usb0 - usb7
        for (int i = 0; i < 8; i++) {
            try {
                File f = new File("/mnt/usb" + i);
                if (f.exists() && f.list((dir, name) -> name.toLowerCase().endsWith(".jar")
                        || name.toLowerCase().endsWith(".elf")
                        || name.toLowerCase().endsWith(".bin")).length > 0) {
                    Status.println("Found usb with payloads on " + f.getAbsolutePath());
                    usbPayloadRoot = f;
                    break;
                }
            } catch (Exception ex) {
                Status.println("Error searching for usb" + i);
            }
        }

        if (usbPayloadRoot != null && usbPayloadRoot.isDirectory() && usbPayloadRoot.canRead()) {
            return usbPayloadRoot;
        }

        Status.println("Pipeline error: could not find any usb payloads");
        return null;
    }
}

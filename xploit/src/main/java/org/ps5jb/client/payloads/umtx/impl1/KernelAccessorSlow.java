package org.ps5jb.client.payloads.umtx.impl1;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ps5jb.loader.KernelAccessor;

/**
 * Basic Kernel read/write accessor which uses reclaim thread from UMTX exploit.
 * This accessor cannot be reused for subsequent JARs.
 */
public class KernelAccessorSlow implements KernelAccessor {
    private static final long serialVersionUID = -7188410401464649957L;

    private transient UmtxExploit owner;

    private KernelAccessorSlow() {
    }

    public KernelAccessorSlow(UmtxExploit owner) {
        this.owner = owner;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        // Do not allow to deserialize this class
        throw new NotActiveException("Slow kernel accessor cannot be restored");
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        // Do not allow to serialize this class
        throw new NotActiveException("Slow kernel accessor cannot be saved");
    }

    @Override
    public byte read1(long kernelAddress) {
        return owner.read8Slow(kernelAddress).byteValue();
    }

    @Override
    public short read2(long kernelAddress) {
        return owner.read16Slow(kernelAddress).shortValue();
    }

    @Override
    public int read4(long kernelAddress) {
        return owner.read32Slow(kernelAddress).intValue();
    }

    @Override
    public long read8(long kernelAddress) {
        return owner.read64Slow(kernelAddress).longValue();
    }

    @Override
    public void write1(long kernelAddress, byte value) {
        owner.write8Slow(kernelAddress, value);
    }

    @Override
    public void write2(long kernelAddress, short value) {
        owner.write16Slow(kernelAddress, value);
    }

    @Override
    public void write4(long kernelAddress, int value) {
        owner.write32Slow(kernelAddress, value);
    }

    @Override
    public void write8(long kernelAddress, long value) {
        owner.write64Slow(kernelAddress, value);
    }

    @Override
    public long getKernelBase() {
        return owner.state == null ? 0 : owner.state.kbaseAddress.addr();
    }
}

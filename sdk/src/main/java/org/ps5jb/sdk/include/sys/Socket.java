package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.inet.in.ProtocolType;
import org.ps5jb.sdk.include.netinet6.in6.OptionIPv6;
import org.ps5jb.sdk.include.sys.socket.AddressFamilyType;
import org.ps5jb.sdk.include.sys.socket.SocketType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/socket.h</code> from FreeBSD source.
 */
public class Socket {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public Socket(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int createSocket(AddressFamilyType domain, SocketType socketType, ProtocolType protocol) throws SdkException {
        int ret = libKernel.socket(domain.value(), socketType.value(), protocol.value());
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "createSocket");
        }
        return ret;
    }

    public void setSocketOptionsIPv6(int socket, OptionIPv6 optionName, Pointer optionValue) throws SdkException {
        long optSize = optionValue.size().longValue();
        int ret = libKernel.setsockopt(socket, ProtocolType.IPPROTO_IPV6.value(), optionName.value(), optionValue, optSize);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "setSocketOptionsIPv6");
        }
    }

    public Pointer getSocketOptionsIPv6(int socket, OptionIPv6 optionName, Pointer optionValue) throws SdkException {
        Pointer optlen = Pointer.calloc(0x4);
        try {
            optlen.write4(optionValue.size().intValue());

            int ret = libKernel.getsockopt(socket, ProtocolType.IPPROTO_IPV6.value(), optionName.value(), optionValue, optlen);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "getSocketOptionsIPv6");
            }

            int newLen = optlen.read4();
            return (newLen == optionValue.size().intValue()) ? optionValue : new Pointer(optionValue.addr(), new Long(newLen));
        } finally {
            optlen.free();
        }
    }
}

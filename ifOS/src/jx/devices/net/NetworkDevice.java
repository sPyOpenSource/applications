package jx.devices.net;

import jx.zero.*;
import jx.fs.buffer.separator.NonBlockingMemoryConsumer;
import jx.devices.Device;

/**
 * @deprecated Extend {@link jx.devices.net.AbstractNetworkDevice} instead of implementing this interface.
 */
@Deprecated
public interface NetworkDevice extends Device, Portal {
    public static final int RECEIVE_MODE_INDIVIDUAL = 1;
    public static final int RECEIVE_MODE_PROMISCOUS = 2;
    public static final int RECEIVE_MODE_MULTICAST  = 3;

    public void setReceiveMode(int mode);
    public Memory transmit(Memory buf);
    public Memory transmit1(Memory buf, int offset, int size);
    public byte[] getMACAddress();
    public int getMTU();
    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer);
}

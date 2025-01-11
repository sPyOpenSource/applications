
package jx.net;

import jx.zero.Memory;

/**
 *
 * @author xuyi
 */
public interface UDPData {

    public void setMemory(Memory mem);

    public void setSourcePort(int srcPort);

    public void setSourceAddress(IPAddress sourceAddress);

    public int Size();
    
    public int getOffset();

    public Memory getMemory();

    public IPAddress getSourceAddress();

    public int getSourcePort();
}

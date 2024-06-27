
package jx.net;

import jx.zero.Memory;

/**
 *
 * @author xuyi
 */
public interface IPData {

    public int Size();
    public int getOffset();
    public Memory getMemory();

    public void setMemory(Memory buf);

    public IPAddress getSourceAddress();

    public void setOffset(int i);

    public IPAddress getDestinationAddress();

    public void setSourceAddress(IPAddress addr);

    public void setDestinationAddress(IPAddress dAddr);

    public void setSize(int i);
    
}

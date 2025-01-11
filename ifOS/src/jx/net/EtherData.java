
package jx.net;

import jx.zero.Memory;

/**
 *
 * @author xuyi
 */
public interface EtherData {
    public Memory getMemory();
    public int getOffset();

    public int Size();

    public void setMemory(Memory newMem);

    public void setOffset(int i);

    public void setSize(int i);

    public void setSrcAddress(byte[] sourceAddress);

    public void setDstAddress(byte[] destAddress);
}


package jx.devices.pci;

import jx.devices.Bus;

/**
 *
 * @author xuyi
 */
public interface PCIDevice extends Bus {

    public short getDeviceID();

    public short getVendorID();

    public byte getInterruptLine();

    public int getBaseAddress(int i);

    public short getCommand();

    public void setCommand(short s);

    public int getClassCode();

    public PCIAddress getAddress();

    public int readConfig(int REG_DEVVEND);

    public byte getHeaderType();

    public int readPackedConfig(int baseRegister, int CAP_ID_MASK, int CAP_ID_SHIFT);

    public void writePackedConfig(int i, int mask, int val);

    public boolean busmasterCapable();

    public boolean enforceBusmaster();

    public void writeConfig(int reg, int iobase);

    public void setBaseAddress(int i, int IOBase);

    public void setLatencyTimer(byte b);

    public int getLatencyTimer();

    public byte getCacheLineSize();

    public byte getRevisionID();

    public int readIRQLine();
    
}

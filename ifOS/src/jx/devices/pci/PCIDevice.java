
package jx.devices.pci;

import jx.devices.Bus;

/**
 *
 * @author xuyi
 */
public interface PCIDevice extends Bus {

    public int getDeviceID();

    public int getVendorID();

    public int getInterruptLine();

    public int getBaseAddress(int i);

    public int getCommand();

    public void setCommand(short s);
    
}

package jx.net.devices.lance;

import jx.devices.net.NetworkDevice;
import jx.zero.*;
import jx.devices.pci.*;
import jx.devices.*;

import jx.buffer.separator.*;

class Lance implements NetworkDevice {
    Lance(PCIDevice dev) {}
    @Override
    public DeviceConfigurationTemplate[] getSupportedConfigurations() {
	return null;
    }

    @Override
    public void open(DeviceConfiguration conf) {}
    @Override
    public void close() {}
    @Override
    public void setReceiveMode(int mode) {}
    @Override
    public Memory transmit(Memory buf) {return null;}
    @Override
    public Memory transmit1(Memory buf, int offset, int size) {return null;}
    @Override
    public byte[] getMACAddress() { return null; }
    @Override
    public int getMTU() {return 0;}
    @Override
    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer){throw new Error();}
}

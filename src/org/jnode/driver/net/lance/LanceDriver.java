/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.driver.net.lance;

//import java.util.logging.Level;
//import java.util.logging.Logger;
import jx.buffer.multithread.Buffer2;
import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.MultiThreadBufferList2;
import jx.buffer.separator.NonBlockingMemoryConsumer;
import jx.devices.DeviceConfiguration;
import jx.devices.DeviceConfigurationTemplate;
import jx.devices.net.NetworkDevice;
import jx.devices.pci.PCIDevice;
import jx.zero.CPUManager;
import jx.zero.Debug;
import jx.zero.IRQ;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Ports;

/**
 * @author epr
 */
public class LanceDriver implements NetworkDevice {

    /*public LanceDriver(ConfigurationElement config) {
        this(new LanceFlags(config));
    }*/
    private LanceCore abstractDeviceCore;
    private final static boolean debugSend = false;
    NonBlockingMemoryConsumer etherConsumer;
    public static final int ETH_DATA_LEN   = 1500;  /* Max. octets in payload */
    private MemoryManager rm;
    private CPUManager cpuManager;
    int event_interrupt;
    int event_snd;
    int event_rcv;
    MultiThreadBufferList usableBufs /*, intransmitBufs*/;

    public LanceDriver(PCIDevice device, LanceFlags flags, IRQ irq, Ports ports, MemoryManager rm, CPUManager cpuManager, Memory[] bufs) {
        this.rm = rm;
        this.cpuManager = cpuManager;
        //this.flags = flags;
        abstractDeviceCore = newCore(device, flags, irq, ports, rm);
        event_interrupt = cpuManager.createNewEvent("Lanceinterrupt");
	event_snd = cpuManager.createNewEvent("LanceSnd");
	event_rcv = cpuManager.createNewEvent("LanceRcv");
        this.usableBufs = new MultiThreadBufferList2(bufs);
	this.usableBufs.enableRecording("Lance-available-queue");
    }

    /**
     * Create a new LanceCore instance
     * @param device
     * @param flags
     * @param irq
     * @param ports
     * @param rm
     * @return 
     */
    protected LanceCore newCore(PCIDevice device, LanceFlags flags, IRQ irq, Ports ports, MemoryManager rm) {
        return new LanceCore(this, device, flags, irq, ports, rm);
    }

    @Override
    public void setReceiveMode(int mode) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Memory transmit(Memory buf) {
        try {
            abstractDeviceCore.transmit(buf);
        } catch (InterruptedException ex) {
            //Logger.getLogger(LanceDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Memory transmit1(Memory buf1, int offset, int size) {
        cpuManager.recordEvent(event_snd);
        Buffer2 h = (Buffer2)usableBufs.nonblockingUndockFirstElement();
	if (h == null) {
	    if (debugSend) Debug.out.println("no usable buffers");
	    return buf1;
	}
	Memory buf2 = h.getRawData();
	if (! buf2.isValid()) {
	    throw new Error();
	}
        Memory buf = rm.alloc(size + offset);
        buf.copyFromMemory(buf1, 0, offset, size);
        try {
            abstractDeviceCore.transmit(buf);
        } catch (InterruptedException ex) {
            //Logger.getLogger(LanceDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return buf2;
    }

    @Override
    public byte[] getMACAddress() {
        return abstractDeviceCore.getHwAddress().get_Addr();
    }

    @Override
    public int getMTU() {
        return ETH_DATA_LEN;
    }

    @Override
    public boolean registerNonBlockingConsumer(NonBlockingMemoryConsumer consumer) {
        if (this.etherConsumer != null) {
	    throw new Error("Consumer already registered.");
	}
	this.etherConsumer = consumer;
	return true;
    }

    @Override
    public DeviceConfigurationTemplate[] getSupportedConfigurations() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void open(DeviceConfiguration conf) {
        abstractDeviceCore.initialize();
    }

    @Override
    public void close() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void onReceive(Memory skbuf) {
        etherConsumer.processMemory(skbuf, 0, skbuf.size());
    }
}

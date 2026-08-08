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
 
package org.jnode.driver.block.usb.storage.scsi;

import bioide.PartitionEntry;
import java.io.IOException;
import jx.devices.Device;
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;

import org.jnode.driver.block.usb.storage.USBStorageConstants;
import org.jnode.driver.block.usb.storage.USBStorageSCSIHostDriver.USBStorageSCSIDevice;

import org.jnode.driver.bus.scsi.SCSIException;
import org.jnode.driver.bus.scsi.cdb.mmc.CapacityData;
import org.jnode.driver.bus.scsi.cdb.mmc.MMCUtils;
import org.jnode.driver.bus.scsi.cdb.spc.SenseData;
import org.jnode.driver.bus.usb.USBRequest;

public class USBStorageSCSIDriver
    implements USBStorageConstants {

    /** */
    //private final FSBlockAlignmentSupport blockAlignment;

    /** */
    private USBStorageSCSIDevice device;

    /** */
    private boolean locked;

    /** */
    private CapacityData capacity;

    /** */
    private int sectorSize = 512;

    /** */
    private boolean changed;

    /** */
    // private final ResourceManager rm;
    public USBStorageSCSIDriver() {
        //this.blockAlignment = new FSBlockAlignmentSupport(this, 2048);
    }

    public void startDevice(Device dev) throws Exception {
        //final Device dev = getDevice();
        this.device = (USBStorageSCSIDevice) dev;
        // Rename the device
        /*try {
            final DeviceManager dm = dev.getManager();
            synchronized (dm) {
                dm.rename(dev, "sg", true);
            }
        } catch (DeviceAlreadyRegisteredException ex) {
            throw new DriverException(ex);
        }*/

        this.locked = false;
        this.changed = true;
        this.capacity = null;
        //this.blockAlignment.setAlignment(2048);

        //dev.registerAPI(RemovableDeviceAPI.class, this);
        //dev.registerAPI(FSBlockDeviceAPI.class, blockAlignment);
    }

    public void stopDevice() throws Exception {
        try {
            unlock();
        } catch (IOException ex) {
            throw new Exception(ex);
        } finally {
            /*final SCSIDevice dev = (SCSIDevice) getDevice();
            dev.unregisterAPI(RemovableDeviceAPI.class);
            dev.unregisterAPI(FSBlockDeviceAPI.class);
            dev.unregisterAPI(SCSIDeviceAPI.class);*/
        }

    }

    public int getSectorSize() throws IOException {
        processChanged();
        if (capacity == null) {
            throw new IOException("No medium");
        }
        return sectorSize;
    }

    public PartitionEntry getPartitionTableEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getLength() throws IOException {
        processChanged();
        if (capacity == null) {
            return 0;
        }
        return (long) sectorSize * ((long) capacity.getLogicalBlockAddress() + 1);
    }

    public void read(long devOffset, byte[] dest) throws IOException {
        processChanged();
        if (capacity == null) {
            throw new IOException("No medium");
        }
        if ((devOffset % sectorSize) != 0) {
            throw new IOException("Unaligned read: offset 0x"
                + Long.toHexString(devOffset) + " is not a multiple of sector size " + sectorSize);
        }

        final int remaining = dest.length;
        final int blocks = (int) ((remaining + sectorSize - 1) / sectorSize);
        final int lba = (int) (devOffset / sectorSize);

        final MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
        final Memory data = rm.alloc(blocks * sectorSize);

        try {
            MMCUtils.readData(device, lba, blocks, sectorSize, data, 0);
            for (int i = 0; i < remaining; i++) {
                dest[i] = data.get8(i);
            }
        } catch (Exception e) {
            throw new IOException("Read failed", e);
        }
    }

    public void write(long devOffset, byte[] src) throws IOException {
        processChanged();
        if (capacity == null) {
            throw new IOException("No medium");
        }
        if ((devOffset % sectorSize) != 0) {
            throw new IOException("Unaligned write: offset 0x"
                + Long.toHexString(devOffset) + " is not a multiple of sector size " + sectorSize);
        }

        final int remaining = src.length;
        final int blocks = (int) ((remaining + sectorSize - 1) / sectorSize);
        final int lba = (int) (devOffset / sectorSize);

        final MemoryManager rm = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
        final Memory data = rm.alloc(blocks * sectorSize);

        for (int i = 0; i < remaining; i++) {
            data.set8(i, src[i]);
        }
        // Zero the tail of the last (partial) sector so no garbage is written
        for (int i = remaining; i < blocks * sectorSize; i++) {
            data.set8(i, (byte) 0);
        }

        try {
            MMCUtils.writeData(device, lba, blocks, sectorSize, data, 0);
        } catch (Exception e) {
            throw new IOException("Write failed", e);
        }
    }

    public void flush() throws IOException {
        try {
            MMCUtils.synchronizeCache(device);
        } catch (Exception e) {
            throw new IOException("Flush failed", e);
        }
    }

    public void requestCompleted(USBRequest request) {
        // TODO Auto-generated method stub

    }

    public void requestFailed(USBRequest request) {
        // TODO Auto-generated method stub

    }

    /**
     * Unlock the device.
     *
     * @throws IOException
     */
    public synchronized void unlock() throws IOException {
        /*if (!locked) {
            final SCSIDevice dev = (SCSIDevice) getDevice();
            try {
                MMCUtils.setMediaRemoval(dev, false, false);
            } catch (Exception ex) {
                final IOException ioe = new IOException();
                ioe.initCause(ex);
                throw ioe;
            }
            locked = false;
        }*/
    }

    private void processChanged() throws IOException {
        if (device == null) {
            throw new IOException("Device not started");
        }
        if (changed) {
            this.capacity = null;
            try {
                // Gets the capacity.
                this.capacity = MMCUtils.readCapacity(device);
                this.sectorSize = capacity.getBlockLength();
                changed = false;
            } catch (SCSIException e) {
                final SenseData sense = e.getSenseData();
                if (sense != null && sense.getSenseKey().isNotReady() && sense.getASC() == 0x3A) {
                    // MEDIUM NOT PRESENT (asc 0x3A)
                    throw new IOException("No medium present");
                }
                if (sense != null && sense.getSenseKey().isUnitAttention() && sense.getASC() == 0x28) {
                    // MEDIUM MAY HAVE CHANGED (asc 0x28) - keep changed, will retry
                    throw new IOException("Media may have changed, retry");
                }
                throw new IOException("Device not ready", e);
            } catch (Exception ex) {
                throw new IOException("Error reading capacity", ex);
            }
        }
    }

    public boolean canLock() {
        return true;
    }

    /**
     * It's a removable device.
     * @return 
     */
    public boolean canEject() {
        return true;
    }

    public void lock() throws IOException {
        // TODO Auto-generated method stub

    }

    public boolean isLocked() {
        return locked;
    }

    public void eject() throws IOException {
        // TODO Auto-generated method stub

    }

    public void load() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

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

import jx.devices.bio.BlockIO;
import jx.fs.FileSystem;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Naming;
import org.jnode.driver.block.usb.storage.USBStorageConstants;
import org.jnode.driver.block.usb.storage.USBStorageSCSIHostDriver;
import org.jnode.driver.bus.usb.InterfaceDescriptor;
import org.jnode.driver.bus.usb.USBDevice;
import org.jnode.driver.bus.usb.USBHubMonitor;
import org.jnode.fs.jfat.FatFileSystem;
import vfs.FSImpl;

/**
 * An attach listener that wires a USB mass storage device (Bulk-Only Transport,
 * SCSI transparent command set) through the storage driver chain and mounts a
 * {@link FatFileSystem} on it via the VFS.
 *
 * <p>Registered with {@link USBHubMonitor#addAttachListener(USBHubMonitor.USBDeviceAttachListener)}.
 * On attach it runs:
 * <pre>
 * USBStorageSCSIHostDriver --startDevice(USBDevice)--&gt; USBStorageSCSIDevice
 * USBStorageSCSIDriver    --startDevice(SCSIDevice)--&gt; block ops
 * USBStorageBlockIO       --wraps USBStorageSCSIDriver--&gt; jx.devices.bio.BlockIO
 * FatFileSystem           --over BlockIO--&gt; jx.fs.FileSystem
 * FSImpl.mountRoot(fat)   --VFS root--&gt; registered as "USBFS" portal
 * </pre>
 *
 * @author Fabien Lesire
 */
public class USBStorageMount implements USBHubMonitor.USBDeviceAttachListener {

    /**
     * Name of the VFS portal registered after a successful mount.
     */
    public static final String FS_PORTAL_NAME = "USBFS";
    /**
     * Name of the BlockIO portal registered after a successful mount.
     */
    public static final String BIO_PORTAL_NAME = "USBBlockIO";

    private static boolean mounted;

    @Override
    public void deviceAttached(USBDevice device) {
        if (mounted) {
            return;
        }
        if (!isMassStorageBulkOnly(device)) {
            return;
        }
        final Naming naming = InitialNaming.getInitialNaming();
        try {
            final USBStorageSCSIHostDriver hostDriver = new USBStorageSCSIHostDriver();
            hostDriver.startDevice(device);
            final USBStorageSCSIDriver scsiDriver = new USBStorageSCSIDriver();
            scsiDriver.startDevice(hostDriver.getScsiDevice());

            final BlockIO bio = new USBStorageBlockIO(scsiDriver);
            Debug.out.println("USBStorageMount: capacity " + bio.getCapacity()
                + " sectors x " + bio.getSectorSize() + " bytes");

            final FileSystem fat = new FatFileSystem(bio);
            final FSImpl fs = new FSImpl();
            fs.mountRoot(fat, false);
            naming.registerPortal(fs, FS_PORTAL_NAME);
            naming.registerPortal(bio, BIO_PORTAL_NAME);
            mounted = true;
            Debug.out.println("USBStorageMount: FatFileSystem mounted, portals '"
                + FS_PORTAL_NAME + "' and '" + BIO_PORTAL_NAME + "' registered");
        } catch (Exception t) {
            Debug.out.println("USBStorageMount: mount failed: " + t);
        }
    }

    /**
     * Returns true if the device presents a Bulk-Only Transport mass storage
     * interface (class 0x08, subclass 0x06 transparent SCSI, protocol 0x50).
     *
     * @param device the enumerated device
     * @return true if it is a BOT mass storage device
     */
    private boolean isMassStorageBulkOnly(USBDevice device) {
        try {
            final org.jnode.driver.bus.usb.USBConfiguration conf = device.getConfiguration(0);
            if (conf == null) {
                return false;
            }
            final InterfaceDescriptor intf = conf.getInterface(0).getDescriptor();
            if (intf.getInterfaceClass() != USBStorageConstants.USB_CLASS_MASS_STORAGE) {
                return false;
            }
            if (intf.getInterfaceSubClass() != USBStorageConstants.US_SC_SCSI) {
                Debug.out.println("USBStorageMount: unsupported sub-class 0x"
                    + Integer.toHexString(intf.getInterfaceSubClass()));
                return false;
            }
            if (intf.getInterfaceProtocol() != USBStorageConstants.US_PR_BULK) {
                Debug.out.println("USBStorageMount: unsupported protocol 0x"
                    + Integer.toHexString(intf.getInterfaceProtocol()));
                return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

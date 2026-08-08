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

import java.io.IOException;
//import java.nio.ByteBuffer;
import jx.devices.bio.BlockIO;
import jx.zero.Memory;

/**
 * Exposes a {@link USBStorageSCSIDriver} as a {@link BlockIO} so that a file
 * system (e.g. {@code org.jnode.fs.jfat.FatFileSystem}) can be mounted on a
 * USB mass storage device via the VFS.
 *
 * @author Fabien Lesire
 */
public class USBStorageBlockIO implements BlockIO {

    private final USBStorageSCSIDriver driver;

    private final int sectorSize;

    /**
     * @param driver
     */
    public USBStorageBlockIO(USBStorageSCSIDriver driver) {
        this.driver = driver;
        try {
            this.sectorSize = driver.getSectorSize();
        } catch (IOException e) {
            throw new Error("Failed to read sector size", e);
        }
    }

    @Override
    public int getCapacity() {
        try {
            return (int) (driver.getLength() / sectorSize);
        } catch (IOException e) {
            throw new Error("Failed to read capacity", e);
        }
    }

    @Override
    public int getSectorSize() {
        return sectorSize;
    }

    @Override
    public void readSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
        final int length = numberOfSectors * sectorSize;
        checkBufferSize(buf, length);
        final byte[] data = new byte[length];
        try {
            driver.read((long) startSector * sectorSize, data);
            buf.copyFromByteArray(data, 0, 0, length);
        } catch (IOException e) {
            throw new Error("Read failed", e);
        }
    }

    @Override
    public void writeSectors(int startSector, int numberOfSectors, Memory buf, boolean synchronous) {
        final int length = numberOfSectors * sectorSize;
        checkBufferSize(buf, length);
        final byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.get8(i);
        }
        try {
            driver.write((long) startSector * sectorSize, data);
        } catch (IOException e) {
            throw new Error("Write failed", e);
        }
    }

    private static void checkBufferSize(Memory buf, int length) {
        if (buf.size() < length) {
            throw new Error("Buffer too small: " + buf.size() + " bytes for " + length + " bytes");
        }
    }
}

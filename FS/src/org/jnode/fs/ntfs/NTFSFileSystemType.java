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
 
package org.jnode.fs.ntfs;

import jx.devices.bio.BlockIO;
import org.jnode.fs.FileSystemType;
import java.nio.file.FileSystemException;

/**
 * @author Chira
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class NTFSFileSystemType implements FileSystemType<NTFSFileSystem> {
    public static final Class<NTFSFileSystemType> ID = NTFSFileSystemType.class;
    public static final String TAG = "NTFS    ";

    public String getName() {
        return "NTFS";
    }

    /**
     * @see org.jnode.fs.BlockDeviceFileSystemType#supports(org.jnode.partitions.PartitionTableEntry,
     * byte[], org.jnode.driver.block.FSBlockDeviceAPI) 
     */
    public boolean supports(byte[] firstSectors) {
        if (firstSectors.length < 0x11) {
            // Not enough data for detection
            return false;
        }

        // Intentionally not checking the PARTTYPE because that often lies.
        return new String(firstSectors, 0x03, 8).equals(TAG);
    }

    /**
     * @see org.jnode.fs.FileSystemType#create(Device, boolean)
     */
    public NTFSFileSystem create(BlockIO device, boolean readOnly) throws FileSystemException {
        return new NTFSFileSystem(device, readOnly, this);
    }
}

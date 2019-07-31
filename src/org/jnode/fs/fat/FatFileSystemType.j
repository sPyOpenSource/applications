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
 
package org.jnode.fs.fat;

import jx.bio.BlockIO;
import org.jnode.fs.BlockDeviceFileSystemType;
//import org.jnode.fs.FileSystemException;
import bioide.PartitionEntry;

/**
 * @author epr
 */
public class FatFileSystemType implements BlockDeviceFileSystemType<FatFileSystem> {
    public static final Class<FatFileSystemType> ID = FatFileSystemType.class;

    /**
     * Gets the unique name of this file system type.
     */
    public String getName() {
        return "FAT";
    }

    /**
     * Can this file system type be used on the given first sector of a
     * blockdevice?
     * 
     * @param pte The partition table entry, if any. If null, there is no
     *            partition table entry.
     * @param firstSector
     */
    public boolean supports(PartitionEntry pte, byte[] firstSector, BlockIO devApi) {
/*
        if (pte != null) {
            if (!pte.isValid()) {
                return false;
            }
            if (!(pte instanceof IBMPartitionTableEntry)) {
                return false;
            }
            final IBMPartitionTableEntry ipte = (IBMPartitionTableEntry) pte;
            final IBMPartitionTypes type = ipte.getSystemIndicator();
            if ((type == IBMPartitionTypes.PARTTYPE_DOS_FAT12) ||
                    (type == IBMPartitionTypes.PARTTYPE_DOS_FAT16_LT32M) ||
                    (type == IBMPartitionTypes.PARTTYPE_DOS_FAT16_GT32M)) {
                return true;
            } else {
                return false;
            }
        }

        try
        {
        if (!new BootSector(firstSector).isaValidBootSector())
            return false;
        }
        catch (RuntimeException e)
        {
            return false;
        }
*/

        // FAT-32 is currently handled by the newer jfat package.
        return (firstSector[38] == 0x29 &&
                firstSector[54] == 'F' &&
                firstSector[55] == 'A' &&
                firstSector[56] == 'T');
    }

    /**
     * Create a filesystem for a given device.
     *
     * @param device
     * @param readOnly
     */
    public FatFileSystem create(boolean readOnly)// throws FileSystemException 
    {
        return new FatFileSystem(readOnly, this);
    }
}

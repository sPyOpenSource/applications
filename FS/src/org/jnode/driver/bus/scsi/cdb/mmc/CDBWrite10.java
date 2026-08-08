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
 
package org.jnode.driver.bus.scsi.cdb.mmc;

import org.jnode.driver.bus.scsi.CDB;


/**
 * CDB for an WRITE (10) command.
 * See SCSI Multimedia Commands-4, section 6.32.
 * Data direction is host to device (OUT).
 *
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class CDBWrite10 extends CDB {

    private final int nrBlocks;
    private final int blockSize;

    /**
     * Initialize this instance.
     *
     * @param lba       Logical block address of first block that will be written.
     * @param nrBlocks  The number of blocks that will be written.
     * @param blockSize The size of a single block in bytes.
     */
    public CDBWrite10(int lba, int nrBlocks, int blockSize) {
        super(10, 0x2A);
        this.nrBlocks = nrBlocks;
        this.blockSize = blockSize;
        setInt32(2, lba);
        setInt16(7, nrBlocks);
    }

    @Override
    public int getDataTransfertCount() {
        return nrBlocks * blockSize;
    }

}

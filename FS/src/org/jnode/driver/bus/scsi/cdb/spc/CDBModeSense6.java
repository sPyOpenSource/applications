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
 
package org.jnode.driver.bus.scsi.cdb.spc;

import org.jnode.driver.bus.scsi.CDB;


/**
 * CDB for a MODE SENSE (6) command.
 * See SCSI Primary Commands-3, section 6.9.
 * Data direction is device to host (IN).
 *
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class CDBModeSense6 extends CDB {

    private final int allocLen;

    /**
     * Initialize this instance.
     *
     * @param pageCode The mode page code to be returned (bits 5-0 of byte 2).
     * @param allocLen The maximum number of bytes to be returned.
     */
    public CDBModeSense6(int pageCode, int allocLen) {
        super(6, 0x1A);
        this.allocLen = Math.min(allocLen, 0xFF);
        setInt8(2, pageCode & 0x3F);
        setInt8(4, this.allocLen);
    }

    @Override
    public int getDataTransfertCount() {
        return allocLen;
    }

}

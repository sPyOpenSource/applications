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
 * CDB for a MODE SELECT (6) command.
 * See SCSI Primary Commands-3, section 6.8.
 * Data direction is host to device (OUT).
 *
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class CDBModeSelect6 extends CDB {

    private final int paramLen;

    /**
     * Initialize this instance.
     *
     * @param paramLen The length of the mode parameter list to be transferred.
     */
    public CDBModeSelect6(int paramLen) {
        super(6, 0x15);
        this.paramLen = Math.min(paramLen, 0xFF);
        setInt8(1, 0x08); // PF=1 (page format)
        setInt8(4, this.paramLen);
    }

    @Override
    public int getDataTransfertCount() {
        return paramLen;
    }

}

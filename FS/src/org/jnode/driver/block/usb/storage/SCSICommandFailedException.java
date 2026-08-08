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
 
package org.jnode.driver.block.usb.storage;

import org.jnode.driver.bus.usb.USBException;
import org.jnode.util.NumberUtils;

public class SCSICommandFailedException extends USBException {
    private final int status;
    private final int residue;

    public SCSICommandFailedException(int status, int residue) {
        super("SCSI command failed with CSW status: 0x" + NumberUtils.hex(status, 2));
        this.status = status;
        this.residue = residue;
    }

    public int getStatus() {
        return status;
    }

    public int getResidue() {
        return residue;
    }
}

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

import jx.zero.Memory;
import org.jnode.driver.bus.scsi.SCSIBuffer;

/**
 * Mode parameter header wrapper for the response to a MODE SENSE(6) command.
 * See SCSI Primary Commands-3, section 6.11.
 *
 * @author Fabien Lesire
 */
public class ModePageData {

    /**
     * A sensible allocation length for a MODE SENSE(6) response.
     */
    public static final int DEFAULT_LENGTH = 256;

    private final SCSIBuffer buffer;

    /**
     * Initialize this instance from a mode sense response.
     *
     * @param data
     */
    public ModePageData(Memory data) {
        this.buffer = new SCSIBuffer(data);
    }

    /**
     * Gets the device-specific parameter byte. Bit 7 is the write protect flag.
     */
    public final int getDeviceSpecificParameter() {
        return buffer.getUInt8(2);
    }

    /**
     * Is the medium write protected?
     */
    public final boolean isWriteProtected() {
        return (getDeviceSpecificParameter() & 0x80) != 0;
    }

}

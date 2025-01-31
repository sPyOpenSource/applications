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

import jx.zero.Memory;
import org.jnode.driver.bus.scsi.SCSIBuffer;
import org.jnode.util.NumberUtils;

/**
 * Fixed format Sense data wrapper.
 * See SCSI Primary Commands-3, section 4.5.3.
 *
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class SenseData {

    private final SenseKey senseKey;
    private final int responseCode;
    private final int asc;
    private final int ascq;

    /**
     * Initialize this instance from a given format sense data response.
     *
     * @param buffer
     */
    public SenseData(Memory buffer) {
        this.responseCode = new SCSIBuffer(buffer).getUInt8(0) & 0x7F;
        this.senseKey = SenseKey.valueOf(new SCSIBuffer(buffer).getUInt8(2) & 0x0F);
        this.asc = new SCSIBuffer(buffer).getUInt8(12);
        this.ascq = new SCSIBuffer(buffer).getUInt8(13);
    }

    /**
     * Gets the response code. See section 4.5.1.
     */
    public final int getResponseCode() {
        return responseCode;
    }

    /**
     * Gets the sense key. See section 4.5.2.1.
     */
    public final SenseKey getSenseKey() {
        return senseKey;
    }

    /**
     * Gets the additonal sense code. See section 4.5.2.1.
     */
    public final int getASC() {
        return asc;
    }

    /**
     * Gets the additonal sense code qualifier. See section 4.5.2.1.
     */
    public final int getASCQ() {
        return ascq;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Response code 0x" + NumberUtils.hex(getResponseCode(), 2) + ", "
            + getSenseKey() + ", ASC 0x" + NumberUtils.hex(getASC(), 2)
            + ", ASCQ 0x" + NumberUtils.hex(getASCQ(), 2);
    }

}

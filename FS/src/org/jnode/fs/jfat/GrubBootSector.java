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
 
package org.jnode.fs.jfat;

import jx.zero.Memory;
import org.jnode.fs.jfat.BootSector;

/**
 * <description>
 * 
 * @author epr
 */
public class GrubBootSector extends BootSector {

    /**
     * Constructor for GrubBootSector.
     * @param size
     */
    public GrubBootSector(Memory size) {
        super(size);
    }

    /**
     * Gets the first sector of stage2 
     * @return long
     */
    public long getStage2Sector() {
        return get32(0x44);
    }

    /**
     * Sets the first sector of stage2
     */
    public void setStage2Sector(long v) {
        set32(0x44, v);
    }

}

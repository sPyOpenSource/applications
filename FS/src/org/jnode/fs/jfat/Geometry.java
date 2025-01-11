
package org.jnode.fs.jfat;

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

/**
 * @author epr
 */
public class Geometry {

    private final int cylinders;
    private final int heads;
    private final int sectors;

    /**
     * Create a new instance
     *
     * @param c The number of cylinders
     * @param h The number of heads
     * @param s The number of sectors/cylinder
     */
    public Geometry(int c, int h, int s) {
        this.cylinders = c;
        this.heads = h;
        this.sectors = s;
    }

    /**
     * @return int
     */
    public int getCylinders() {
        return cylinders;
    }

    /**
     * @return int
     */
    public int getHeads() {
        return heads;
    }

    /**
     * @return int
     */
    public int getSectors() {
        return sectors;
    }

    /**
     * Gets the total number of sectors
     *
     * @return int
     */
    public long getTotalSectors() {
        long v = cylinders;
        v *= heads;
        v *= sectors;
        return v;
    }

    /**
     * Gets the logical sector number for a given CHS.
     *
     * @param chs
     * @return long
     */
    public long getLogicalSector(CHS chs) {
        //ls = c*H*S + h*S + s - 1
        long v = chs.getCylinder() * heads * sectors;
        v += chs.getHead() * sectors;
        v += chs.getSector();
        return v - 1;
    }

    /**
     * Gets a CHS from a given logical sector number
     *
     * @param logicalSector
     * @return CHS
     * @throws GeometryException
     */
    public CHS getCHS(long logicalSector) throws Exception {
        // ls = (c*H + h) * S + s - 1

        long v = logicalSector;
        int s = (int) ((v % sectors) + 1);
        v = v / sectors;
        int h = (int) (v % heads);
        v = v / heads;
        int c = (int) v;

        try {
            return new CHS(c, h, s);
        } catch (IllegalArgumentException iae) {
            throw new Exception("can't get CHS for logical sector " + logicalSector, iae);
        }
    }

    /**
     * increments the given sector and returns the next logical sector as CHS-value
     *
     * @param chsToIncrement
     * @return the CHS value of next sector
     * @throws GeometryException
     */
    public CHS nextSector(CHS chsToIncrement) throws Exception {

        int s = chsToIncrement.getSector();
        int h = chsToIncrement.getHead();
        int c = chsToIncrement.getCylinder();

        s++;

        if (s > sectors) {
            s = 1;
            h++;
            if (h >= heads) {
                h = 0;
                c++;
                if (c >= cylinders) {
                    throw new Exception("this geometry doesn't support cyclinder" + c);
                }
            }
        }
        return new CHS(c, h, s);
    }
    
    class CHS {

    private final int cylinder;
    private final int head;
    private final int sector;

    /**
     * @param c
     * @param h
     * @param s
     * @throws IllegalArgumentException
     */
    public CHS(int c, int h, int s) throws IllegalArgumentException {
        if (c < 0) {
            throw new IllegalArgumentException("Cylinder < 0");
        }
        if (h < 0) {
            throw new IllegalArgumentException("Head < 0");
        }
        if (s < 1) {
            throw new IllegalArgumentException("Sector < 1");
        }
        this.cylinder = c;
        this.head = h;
        this.sector = s;
    }

    /**
     * @return int
     */
    public int getCylinder() {
        return cylinder;
    }

    /**
     * @return int
     */
    public int getHead() {
        return head;
    }

    /**
     * @return int
     */
    public int getSector() {
        return sector;
    }

    /**
     * @param obj
     * @return boolean
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof CHS)) {
            CHS o = (CHS) obj;
            return (cylinder == o.cylinder) && (head == o.head) && (sector == o.sector);
        } else {
            return false;
        }
    }

    /**
     * @return String
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "" + cylinder + "/" + head + "/" + sector;
    }

}
}
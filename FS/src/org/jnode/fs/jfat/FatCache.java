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

import java.io.IOException;
import jx.fs.buffer.BufferCache;
import jx.fs.buffer.BufferHead;
import jx.zero.Debug;
import jx.zero.Memory;

public class FatCache implements BufferCache {

    public final static float loadFactor = 0.75f;

    private final Fat fat;
    private final int fatsize;
    private final int nrfats;

    public int elementSize;

    private final CacheElement[] elements;
    private final int[] keys;
    private int nextVictim;

    // JCore cannot execute long arithmetic in domain code.
    private int access = 0;
    private int hit = 0;

    public FatCache(Fat fat, int cacheSize, int elementSize) {
        this.fat = fat;
        this.fatsize =
        fat.getBootSector().getSectorsPerFat() * fat.getBootSector().getBytesPerSector();
        this.nrfats = fat.getBootSector().getNrFats();
        this.elementSize = elementSize;

        // Use a small fixed-size cache.  The runtime's LinkedHashMap cannot
        // safely handle frequent put() calls, and reading every FAT sector
        // from USB makes large directory scans unusably slow.
        int size = cacheSize;
        if (size > 32)
            size = 32;
        if (size < 1)
            size = 1;
        this.elements = new CacheElement[size];
        this.keys = new int[size];
        for (int i = 0; i < size; i++) {
            this.keys[i] = -1;
            this.elements[i] = new CacheElement(fat.getApi(),
                fat.getBootSector().getSectorsPerFat(), fat.getBootSector().getNrFats());
        }
        this.nextVictim = 0;
    }

    public int getCacheSize() {
        return elements.length;
    }

    public int usedEntries() {
        int used = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != -1)
                used++;
        }
        return used;
    }

    public int freeEntries() {
        return elements.length - usedEntries();
    }

    private CacheElement get(int address) throws IOException {
        access++;
        for (int i = 0; i < elements.length; i++) {
            if (keys[i] == address) {
                hit++;
                return elements[i];
            }
        }

        int victim = nextVictim;
        CacheElement c = elements[victim];
        if (keys[victim] != -1)
            c.flush();
        c.read(address);
        keys[victim] = address;
        nextVictim++;
        if (nextVictim >= elements.length)
            nextVictim = 0;
        return c;
    }

    public int getUInt16(int index) throws IOException {
        synchronized (fat) {
            return getUInt16Locked(index);
        }
    }

    private int getUInt16Locked(int index) throws IOException {
        int offset = fat.position(0, index);
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        Memory data = get(addr).getData();
        return (data.get8(ofs) & 0xFF) | ((data.get8(ofs + 1) & 0xFF) << 8);
    }

    public int getUInt32(int index) throws IOException {
        synchronized (fat) {
            return getUInt32Locked(index);
        }
    }

    private int getUInt32Locked(int index) throws IOException {
        int offset = fat.position(0, index);
        if (offset < 0) {
            return 0x0FFFFFFF;
        }
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);
        Memory data = get(addr).getData();
        return data.getLittleEndian32(ofs);
    }

    public void setInt16(int index, int value) throws IOException {
        synchronized (fat) {
            setInt16Locked(index, value);
        }
    }

    private void setInt16Locked(int index, int value) throws IOException {
        int offset = fat.position(0, index);
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        CacheElement c = get(addr);
        Memory data = c.getData();

        data.setLittleEndian16(ofs, (short) value);

        c.setDirty();
    }

    public void setInt32(int offset, int value) throws IOException {
        synchronized (fat) {
            setInt32Locked(offset, value);
        }
    }

    private void setInt32Locked(int offset, int value) throws IOException {
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        CacheElement c = get(addr);
        Memory data = c.getData();

        data.setLittleEndian32(ofs, value);

        c.setDirty();
    }

    /*public int getUInt16(int index) throws IOException {
        return getUInt16(fat.position(0, index));
    }

    public void setInt16(int index, int element) throws IOException {
        setInt16(fat.position(0, index), element);
    }

    public void setInt32(int index, int element) throws IOException {
        setInt32(fat.position(0, index), element);
    }*/

    public void flush(int address) throws IOException {
        for (int i = 0; i < elements.length; i++) {
            if (keys[i] == address) {
                elements[i].flush();
                return;
            }
        }
    }

    public void flush() throws IOException {
        for (int i = 0; i < elements.length; i++) {
            elements[i].flush();
        }
    }

    public int getHit() {
        return hit;
    }

    public int getAccess() {
        return access;
    }

    public double getRatio() {
        if (access > 0)
            return ((double) hit / (double) access);
        else
            return 0.0f;
    }

    public String flushOrder() {
        return "";//map.flushOrder();
    }

    /*public String toString() {
        StrWriter out = new StrWriter();

        out.print(map);
        out.println("size=" + getCacheSize() + " used=" + usedEntries() + " free=" + freeEntries());

        return out.toString();
    }*/

    @Override
    public void syncDevice(boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void flushCache() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void showBuffers() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public BufferHead bread(int bg_inode_table) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public BufferHead getblk(int nr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void bdwrite(BufferHead bh) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void brelse(BufferHead bh) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

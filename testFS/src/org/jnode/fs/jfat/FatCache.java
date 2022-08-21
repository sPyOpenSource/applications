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
import jx.zero.Debug;
import jx.zero.Memory;

public class FatCache {

    public final static float loadFactor = 0.75f;

    private final Fat fat;
    private final int fatsize;
    private final int nrfats;

    public int elementSize;

    private final CacheMap map;

    private long access = 0;
    private long hit = 0;

    public FatCache(Fat fat, int cacheSize, int elementSize) {
        this.fat = fat;
        this.fatsize =
        fat.getBootSector().getSectorsPerFat() * fat.getBootSector().getBytesPerSector();
        this.nrfats = fat.getBootSector().getNrFats();
        this.elementSize = elementSize;

        // allocate the LinkedHashMap
        // that do the dirty LRU job
        this.map = new CacheMap(cacheSize, fat.getApi());
    }

    public int getCacheSize() {
        return map.getCacheSize();
    }

    public int usedEntries() {
        return map.usedEntries();
    }

    public int freeEntries() {
        return map.freeEntries();
    }

    private CacheElement put(int address) throws IOException {        
        /**
         * get a CacheElement from the stack object pool
         */
        CacheElement c = new CacheElement(fat.getApi());//map.pop();
        Debug.out.println("read");
        /**
         * read the element from the device
         */
        c.read(address);
        
        /**
         * and insert the element into the LinkedHashMap
         */
        //map.put(c);
//Debug.out.println("put");
        /**
         * stack "must" contains at least one entry the placeholder ... so let
         * it throw an exception if this is false
         */
        //CacheElement e = map.peek();
        //Debug.out.println("peek");
        // if an element was discarded from the LRU cache
        // now we can free it ... this will send the element
        // to storage if is marked as dirty
        /*if (!e.isFree())
            e.free();*/
//Debug.out.println("free");
        return c;
    }

    private CacheElement get(int address) throws IOException {
        CacheElement c = map.get(address);
        access++;

        // if the cache contains the element just return it, we have a cache hit
        // this will update the LRU order: the LinkedHashMap will make it the
        // newest
        //
        // the cache element cannot be null so we can avoid to call
        // containsKey();
        //if (c != null)
            hit++;
            // otherwise put a new element inside the cache
            // possibly flushing and discarding the eldest element
       // else
       Debug.out.println("put");
            c = put(address);

        return c;
    }

    public int getUInt16(int offset) throws IOException {
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        Memory data = get(addr).getData();
        return 0;//LittleEndian.getUInt16(data, ofs);
    }

    public int getUInt32(int index) throws IOException {
        int offset = fat.position(0, index);
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);
        Debug.out.println("addr: "+addr);
        Memory data = get(addr).getData();
        Debug.out.println("get ok");
        return data.getLittleEndian32(ofs);
    }

    public void setInt16(int offset, int value) throws IOException {
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        CacheElement c = get(addr);
        Memory data = c.getData();

        //LittleEndian.setInt16(data, ofs, value);

        c.setDirty();
    }

    public void setInt32(int offset, int value) throws IOException {
        int addr = offset / elementSize;
        int ofs = (int) (offset % elementSize);

        CacheElement c = get(addr);
        Memory data = c.getData();

        //LittleEndian.setInt32(data, ofs, value);

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
        CacheElement c = map.get(address);
        if (c != null)
            c.flush();
    }

    public void flush() throws IOException {
        for (CacheElement c : map.values()) {
            c.flush();
        }
    }

    public long getHit() {
        return hit;
    }

    public long getAccess() {
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
}

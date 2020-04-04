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
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Naming;
import org.jnode.fs.fat.FatFileSystem;

public class FatRootDirectory extends FatDirectory {
    private final Naming naming = InitialNaming.getInitialNaming();
    private final MemoryManager rm = (MemoryManager)naming.lookup("MemoryManager");
    private final Memory mem;
    /*
     * for root directory
     */
    public FatRootDirectory(FatFileSystem fs) throws IOException {
        super(fs);
        Fat fat = fs.getFat();
        if (fat.isFat32() || fat.isFat16() || fat.isFat12()) {
            setRoot32((int) getFatFileSystem().getBootSector().getRootDirectoryStartCluster());
        } else {
            throw new UnsupportedOperationException("Unknown Fat Type");
        }
        //scanDirectory();
        BootSector bootSector = getFatFileSystem().getBootSector();

        // Check if this is the end of the root entires
        /*if (index > bootSector.getNrRootDirEntries()) {
            throw new NoSuchElementException();
        }*/
        int rootDirectoryOffset = bootSector.getFirstDataSector();       

        mem = rm.alloc(512);
        getFatFileSystem().getApi().readSectors(rootDirectoryOffset, 1, mem, true);
    }

    @Override
    public FatDirEntry getFatDirEntry(int index, boolean allowDeleted) throws IOException {
        Debug.out.println("index2: "+index);
        if (getFatFileSystem().getFat().isFat32()) {
            // FAT32 uses the FAT to allocate space to the root directory too, so no special handling is required
            //return super.getFatDirEntry(index, allowDeleted);
        }

        //BootSector bootSector = getFatFileSystem().getBootSector();

        // Check if this is the end of the root entires
        /*if (index > bootSector.getNrRootDirEntries()) {
            throw new NoSuchElementException();
        }*/
        //int rootDirectoryOffset = bootSector.getFirstDataSector();       

        //Memory mem = rm.alloc(512);
        //getFatFileSystem().getApi().readSectors(rootDirectoryOffset, 1, mem, true);
        for(int i = 0; i < 10; i++){
                Debug.out.print((char)mem.get8(i*32));
                Debug.out.print((char)mem.get8(i*32+1));
                Debug.out.print((char)mem.get8(i*32+2));
                Debug.out.print((char)mem.get8(i*32+3));
                Debug.out.print((char)mem.get8(i*32+4));
                Debug.out.print((char)mem.get8(i*32+5));
                Debug.out.print((char)mem.get8(i*32+6));
                Debug.out.print((char)mem.get8(i*32+7));
                Debug.out.print((char)mem.get8(i*32+8));
                Debug.out.print((char)mem.get8(i*32+9));
                Debug.out.println((char)mem.get8(i*32+10));
            }
        //index += 1;
        Memory subs = rm.alloc(32);
        subs.copyFromMemory(mem, index * 32, 0, 32);
        Debug.out.print((char)subs.get8(0));
        Debug.out.print((char)subs.get8(1));
        Debug.out.print((char)subs.get8(2));
        Debug.out.print((char)subs.get8(3));
        Debug.out.print((char)subs.get8(4));
        Debug.out.print((char)subs.get8(5));
        Debug.out.print((char)subs.get8(6));
        Debug.out.print((char)subs.get8(7));
        Debug.out.print((char)subs.get8(8));
        Debug.out.print((char)subs.get8(9));
        Debug.out.println((char)subs.get8(10));
        FatMarshal entry = new FatMarshal(subs);
        return createDirEntry(entry, index, allowDeleted);
    }

    @Override
    public String getShortName() {
        return getName();
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public int getIndex() {
        throw new UnsupportedOperationException("Root has not an index");
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public void setName(String newName) throws IOException {
        throw new UnsupportedOperationException("cannot change root name");
    }

    /*public String getLabel() {
        FatShortDirEntry label = getEntry();

        if (label != null)
            return label.getLabel();
        else
            return "";
    }

    public long getCreated() throws IOException {
        FatShortDirEntry label = getEntry();
        return label == null ? 0 : label.getCreated();
    }

    public long getLastModified() throws IOException {
        FatShortDirEntry label = getEntry();
        return label == null ? 0 : label.getLastModified();
    }

    public long getLastAccessed() throws IOException {
        FatShortDirEntry label = getEntry();
        return label == null ? 0 : label.getLastAccessed();
    }*/

    @Override
    public void setCreated(long created) throws IOException {
        throw new UnsupportedOperationException("cannot change root time");
    }

    @Override
    public void setLastModified(long lastModified) throws IOException {
        throw new UnsupportedOperationException("cannot change root time");
    }

    @Override
    public void setLastAccessed(long lastAccessed) throws IOException {
        throw new UnsupportedOperationException("cannot change root time");
    }

    @Override
    public String toString() {
        return String.format("FatRootDirectory [%s]", getName());
    }

    /*public String toDebugString() {
        StrWriter out = new StrWriter();

        out.println("*******************************************");
        out.println("FatRootDirectory");
        out.println("*******************************************");
        out.println(toStringValue());
        out.println("Visited\t\t" + getVisitedChildren());
        out.print("*******************************************");

        return out.toString();
    }*/
}

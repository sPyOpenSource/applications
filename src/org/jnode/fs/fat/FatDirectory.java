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
 
package org.jnode.fs.fat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import jx.bio.BlockIO;
import jx.fs.Inode;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Naming;

/**
 * @author epr
 */
public class FatDirectory extends AbstractDirectory {

    private boolean root = false;
    private String label;
    private Naming naming = InitialNaming.getInitialNaming();
    private MemoryManager rm = (MemoryManager)naming.lookup("MemoryManager");
    /**
     * Constructor for Directory.
     *
     * @param fs
     * @param file
     * @throws java.io.IOException
     */
    public FatDirectory(FatFileSystem fs, FatFile file) throws IOException {
        super(fs, file);
        this.file = file;
        //read();
    }

    // for root
    protected FatDirectory(FatFileSystem fs, int nrEntries) {
        super(fs, nrEntries, null);
        root = true;
    }

    /**
     * Read the contents of this directory from the persistent storage at the
     * given offset.
     * @throws java.io.IOException
     */
    protected synchronized void read() throws IOException {
        entries.setSize((int) file.getLengthOnDisk() / 32);

        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] data = new byte[entries.size() * 32];
        final Memory data = rm.alloc(entries.size() * 32);
        file.read(0, data);
        //read(data);

        resetDirty();
    }

    /**
     * Write the contents of this directory to the given persistent storage at
     * the given offset.
     * @throws java.io.IOException
     */
    protected synchronized void write() throws IOException {
        if (label != null)
            applyLabel();
        // TODO optimize it also to use ByteBuffer at lower level
        final Memory data = null;

        if (canChangeSize(entries.size())) {
            file.setLength(data.size());
        }

        resetDirty();
    }

    public void read(BlockIO device, int offset) throws IOException {
        Debug.out.println(offset);
        Debug.out.println(device.getSectorSize());
        Memory data = rm.allocAligned(entries.size() * 32, 8);
        
        //device.readSectors(offset / device.getSectorSize(), 1, data, true);
        // System.out.println("Directory at offset :" + offset);
        // System.out.println("Length in bytes = " + entries.size() * 32);
        //read(data);
        for(int i = 0; i < entries.size() * 32; i++){
            Debug.out.print(data.get8(i));
        }
        Debug.out.println();
        resetDirty();
    }

    public synchronized void write(BlockIO device, long offset) throws IOException {
        if (label != null)
            applyLabel();
        final Memory data = rm.alloc(entries.size() * 32);
        //write(data);
        device.writeSectors((int)offset, data.size(), data, true);
        resetDirty();
    }

    //@Override
    public Inode getEntryById(String id) throws IOException {
        for (FatBasicDirEntry entry : entries) {
            if (entry != null && entry instanceof FatDirEntry) {
                FatDirEntry fatDirEntry = (FatDirEntry) entry;
                if (fatDirEntry.getId().equals(id)) {
                    return fatDirEntry;
                }
            }
        }

        throw new FileNotFoundException("Failed to find entry with ID: " + id);
    }

    /**
     * Flush the contents of this directory to the persistent storage
     * @throws java.io.IOException
     */
    @Override
    public void flush() throws IOException {
        if (root) {
            final FatFileSystem fs = (FatFileSystem) getFileSystem();
            if (fs != null) {
                //long offset = FatUtils.getRootDirOffset(fs.getBootSector());
                //write(fs.getApi(), offset);
            }
        } else {
            write();
        }
    }

    /**
     * @return 
     * @see org.jnode.fs.fat.AbstractDirectory#canChangeSize(int)
     */
    @Override
    protected boolean canChangeSize(int newSize) {
        return !root;
    }

    /**
     * Set the label
     *
     * @param label
     * @throws java.io.IOException
     */
    public void setLabel(String label) throws IOException {
        if (!root) {
            throw new IOException("You cannot change the volume name on a non-root directory");
        }
        this.label = label;
    }

    private void applyLabel() throws IOException {
        FatDirEntry labelEntry = null;
        Iterator<Inode> i = iterator();
        FatDirEntry current;
        while (labelEntry == null && i.hasNext()) {
            current = (FatDirEntry) i.next();
            if (current.isLabel() &&
                !(current.isHidden() && current.isReadonly() && current.isSystem())) {
                labelEntry = current;
            }
        }
        if (labelEntry == null) {
            labelEntry = addFatFile(label);
            labelEntry.setLabel();
        }
        labelEntry.setName(label);
        if (label.length() > 8) {
            labelEntry.setExt(label.substring(8));
        } else {
            labelEntry.setExt("");
        }
    }
}

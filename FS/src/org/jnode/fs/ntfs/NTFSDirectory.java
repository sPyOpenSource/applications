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
 
package org.jnode.fs.ntfs;

import java.io.IOException;
import java.util.Iterator;
import java.nio.file.ReadOnlyFileSystemException;

import jx.fs.FileSystem;
import jx.fs.Node;
import org.jnode.fs.ntfs.index.NTFSIndex;

/**
 * A NTFS directory.
 *
 * @author vali
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 * @author Luke Quinane
 */
public class NTFSDirectory extends NTFSEntry {

    //private static final Logger log = Logger.getLogger(NTFSDirectory.class);

    /**
     * The file record for the directory.
     */
    private final FileRecord fileRecord;

    /**
     * The directory's index.
     */
    private final NTFSIndex index;

    /**
     * The associated file system.
     */
    private final NTFSFileSystem fs;

    /**
     * The ID for this directory.
     */
    private final String id;

    /**
     * Initialize this instance.
     *
     * @param record the file record.
     */
    public NTFSDirectory(NTFSFileSystem fs, FileRecord record) throws IOException {
        this.fs = fs;
        this.fileRecord = record;
        this.index = new NTFSIndex(record, "$I30");
        id = Long.toString(record.getReferenceNumber());
    }

    public String getDirectoryId() {
        return id;
    }

    /**
     * Gets an iterator to iterate over all entries of this directory.
     */
    public Iterator<Node> iterator() {
        return new DirectoryEntryIterator(fs, index);
    }

    /**
     * Gets an entry with a given name.
     */
    public Node getEntry(String name) {
        //log.debug("getEntry(" + name + ")");
        for (Iterator<Node> it = this.iterator(); it.hasNext(); ) {
            final NTFSEntry entry = (NTFSEntry) it.next();
            if (entry.getName().equals(name)) {
                return entry;
            }
        }
        return null;
    }

    public Node getEntryById(String id) throws IOException {
        FileRecord fileRecord = fs.getNTFSVolume().getMFT().getRecord(Long.parseLong(id));
        return new NTFSEntry(fs, fileRecord, this.fileRecord.getReferenceNumber());
    }

    /**
     * Gets the NTFS file record for the directory.
     *
     * @return the file record.
     */
    @Override
    public FileRecord getFileRecord() {
        return fileRecord;
    }

    /**
     *
     */
    public Node addFile(String name) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    /**
     *
     */
    public Node addDirectory(String name) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    /**
     * Remove the entry with the given name from this directory.
     */
    public void remove(String name) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    /**
     * Is this entry valid.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     *
     */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Save all dirty (unsaved) data to the device
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        //TODO
    }
}

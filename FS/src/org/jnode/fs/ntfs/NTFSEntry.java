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
import jx.fs.DirNotEmptyException;
import jx.fs.FileExistsException;
import jx.fs.Node;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoFileInodeException;
import jx.fs.NoSymlinkInodeException;
import jx.fs.NotExistException;
import jx.fs.NotSupportedException;
import jx.fs.PermissionException;
import jx.fs.StatFS;
import jx.fs.FileSystem;
import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;

import org.jnode.fs.ntfs.attribute.NTFSAttribute;
import org.jnode.fs.ntfs.index.IndexEntry;

/**
 * @author vali
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class NTFSEntry implements Node {

    private Node cachedFSObject;

    /**
     * The ID for this entry.
     */
    private final String id;

    /**
     * The index entry.
     */
    private IndexEntry indexEntry;

    /**
     * The associated file record.
     */
    private FileRecord fileRecord;

    /**
     * The parent reference number.
     */
    private long parentReferenceNumber = -1;

    /**
     * The cached file name.
     */
    private String name;

    /**
     * The containing file system.
     */
    private final NTFSFileSystem fs;

    /**
     * Initialize this instance.
     *
     * @param fs         the file system.
     * @param indexEntry the index entry.
     */
    public NTFSEntry(NTFSFileSystem fs, IndexEntry indexEntry) {
        this.fs = fs;
        this.indexEntry = indexEntry;
        id = Long.toString(indexEntry.getFileReferenceNumber());
    }

    /**
     * Initialize this instance.
     *
     * @param fs         the file system.
     * @param fileRecord the file record.
     * @param parentReferenceNumber the parent reference number.
     */
    public NTFSEntry(NTFSFileSystem fs, FileRecord fileRecord, long parentReferenceNumber) {
        this.fs = fs;
        this.fileRecord = fileRecord;
        id = Long.toString(fileRecord.getReferenceNumber());
        this.parentReferenceNumber = parentReferenceNumber;
    }

    public String getId() {
        return id;
    }

    /**
     * Gets the name of this entry.
     *
     * @see org.jnode.fs.FSEntry#getName()
     */
    public String getName() {
        if (name != null) {
            return name;
        }

        if (indexEntry != null) {
            FileNameAttribute.Structure fileName = new FileNameAttribute.Structure(
                indexEntry, IndexEntry.CONTENT_OFFSET);
            name = fileName.getFileName();
        } else if (fileRecord != null) {
            if (parentReferenceNumber != -1) {
                // The file name can be different for every hard-linked copy of the file. To find the correct name
                // look for a matching parent MFT index
                FileNameAttribute fileNameAttribute = null;
                Iterator<NTFSAttribute> iterator = fileRecord.findAttributesByType(NTFSAttribute.Types.FILE_NAME);
                while (iterator.hasNext()) {
                    FileNameAttribute attribute = (FileNameAttribute) iterator.next();

                    if (attribute.getParentMftIndex() != parentReferenceNumber) {
                        // File name attribute doesn't match our current parent
                        continue;
                    }

                    // Prefer the win32 namespace
                    if (fileNameAttribute == null ||
                        fileNameAttribute.getNameSpace() != FileNameAttribute.NameSpace.WIN32) {
                        fileNameAttribute = attribute;
                    }
                }

                if (fileNameAttribute != null) {
                    name = fileNameAttribute.getFileName();
                }
            }

            if (name == null) {
                // Didn't find a matching parent, just return the 'best' name
                name = fileRecord.getFileName();
            }
        }

        return name;
    }

    /**
     * @see org.jnode.fs.FSEntry#getParent()
     */
    @Override
    public NTFSDirectory getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getCreated() throws IOException {
        if (getFileRecord().getStandardInformationAttribute() == null) {
            return 0;
        } else {
            return NTFSUTIL.filetimeToMillis(getFileRecord().getStandardInformationAttribute().getCreationTime());
        }
    }

    public long getLastModified() throws IOException {
        if (getFileRecord().getStandardInformationAttribute() == null) {
            return 0;
        } else {
            return NTFSUTIL.filetimeToMillis(getFileRecord().getStandardInformationAttribute().getModificationTime());
        }
    }

    public long getLastChanged() throws IOException {
        if (getFileRecord().getStandardInformationAttribute() == null) {
            return 0;
        } else {
            return NTFSUTIL.filetimeToMillis(getFileRecord().getStandardInformationAttribute().getMftChangeTime());
        }
    }

    public long getLastAccessed() throws IOException {
        if (getFileRecord().getStandardInformationAttribute() == null) {
            return 0;
        } else {
            return NTFSUTIL.filetimeToMillis(getFileRecord().getStandardInformationAttribute().getAccessTime());
        }
    }

    /**
     * @see org.jnode.fs.FSEntry#isFile()
     */
    @Override
    public boolean isFile() {
        if (indexEntry != null) {
            FileNameAttribute.Structure fileName = new FileNameAttribute.Structure(
                indexEntry, IndexEntry.CONTENT_OFFSET);
            return !fileName.isDirectory();
        } else {
            return !fileRecord.isDirectory();
        }
    }

    /**
     * @see org.jnode.fs.FSEntry#isDirectory()
     */
    @Override
    public boolean isDirectory() {
        if (indexEntry != null) {
            FileNameAttribute.Structure fileName = new FileNameAttribute.Structure(
                indexEntry, IndexEntry.CONTENT_OFFSET);
            return fileName.isDirectory();
        } else {
            return fileRecord.isDirectory();
        }
    }

    /**
     * @see org.jnode.fs.FSEntry#setName(java.lang.String)
     */
    public void setName(String newName) {
        // TODO Auto-generated method stub

    }

    public void setCreated(long created) {
        // TODO: Implement write support.
    }

    public void setLastModified(long lastModified) {
        // TODO: Implement write support.
    }

    public void setLastAccessed(long lastAccessed) {
        // TODO: Implement write support.
    }

    /**
     * @see org.jnode.fs.FSEntry#getFile()
     */
    public NTFSFile getFile() {
        if (this.isFile()) {
            if (cachedFSObject == null) {
                if (indexEntry != null) {
                    cachedFSObject = new NTFSFile(fs, indexEntry);
                } else {
                    cachedFSObject = new NTFSFile(fs, fileRecord);
                }
            }
            return (NTFSFile) cachedFSObject;
        } else {
            return null;
        }
    }

    /**
     * @see org.jnode.fs.FSEntry#getDirectory()
     */
    public NTFSDirectory getDirectory() throws IOException {
        if (this.isDirectory()) {
            if (cachedFSObject == null) {
                if (fileRecord != null) {
                    cachedFSObject = new NTFSDirectory(fs, fileRecord);
                } else {
                    // XXX: Why can't this just use getFileRecord()?
                    cachedFSObject = new NTFSDirectory(fs, getFileRecord().getVolume().getMFT().getIndexedFileRecord(
                        indexEntry));
                }
            }
            return (NTFSDirectory) cachedFSObject;
        } else return null;
    }

    /**
     * @see org.jnode.fs.FSEntry#getAccessRights()
     */
    /*public FSAccessRights getAccessRights() {
        // TODO Auto-generated method stub
        return null;
    }*/

    /**
     * @see org.jnode.fs.FSObject#isValid()
     */
    public boolean isValid() {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * @see org.jnode.fs.FSObject#getFileSystem()
     */
    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * @return Returns the fileRecord.
     */
    public FileRecord getFileRecord() throws IOException {
        if (fileRecord != null) {
            return fileRecord;
        }
        return indexEntry.getParentFileRecord().getVolume().getMFT().getIndexedFileRecord(indexEntry);
    }

    /**
     * @return Returns the indexEntry.
     */
    public IndexEntry getIndexEntry() {
        return indexEntry;
    }

    /**
     * Indicate if the entry has been modified in memory (ie need to be saved)
     *
     * @return true if the entry need to be saved
     */
    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public String toString() {
        Object obj = indexEntry == null ? fileRecord : indexEntry;
        return super.toString() + '(' + obj + ')';
    }

    @Override
    public void setParent(Node parent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDirty(boolean value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void incUseCount() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void decUseCount() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int i_nlinks()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteNode()// throws InodeIOException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeNode()// throws InodeIOException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putNode()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void overlay(Node newChild, String name)// throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeOverlay(Node child)// throws InodeNotFoundException, NoDirectoryInodeException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAllOverlays()// throws NoDirectoryInodeException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isOverlayed(String name)// throws NoDirectoryInodeException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node lookup(String name)// throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSymlink()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isWritable()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isReadable()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isExecutable()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastModified()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastAccessed()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastChanged()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLastModified(int time)// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLastAccessed(int time)// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] readdirNames()// throws NoDirectoryInodeException, NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getNode(String name)// throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node mkdir(String name, int mode)// throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rmdir(String name)// throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node create(String name, int mode)// throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlink(String name)// throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node symlink(String symname, String newname)// throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSymlink()// throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rename(String oldname, Node new_dir, String newname)// throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(Memory mem, int off, int len)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(int pos, Memory mem, int bufoff, int len)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyMemory readWeak(int off, int len)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int write(Memory mem, int off, int len)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int write(int pos, Memory mem, int bufoff, int len)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int available()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLength()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getIdentifier()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getVersion()// throws NotExistException 
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StatFS getStatFS() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

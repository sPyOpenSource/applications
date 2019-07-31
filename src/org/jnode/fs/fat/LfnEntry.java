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

import java.io.IOException;
import java.util.Vector;
import jx.fs.DirNotEmptyException;
//import org.jnode.fs.FSAccessRights;
import jx.fs.Directory;
import jx.fs.FileExistsException;
import jx.fs.Inode;
//import org.jnode.fs.FSEntryCreated;
//import org.jnode.fs.FSEntryLastAccessed;
import jx.fs.FileSystem;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoFileInodeException;
import jx.fs.NoSymlinkInodeException;
import jx.fs.NotExistException;
import jx.fs.NotSupportedException;
import jx.fs.PermissionException;
import jx.fs.RegularFile;
import jx.fs.StatFS;
import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;

/**
 * @author gbin
 */
class LfnEntry implements Inode//, FSEntryCreated, FSEntryLastAccessed 
{

    /**
     * The ID for this entry.
     */
    private final String id;

    // decompacted LFN entry
    private String fileName;
    // TODO: Make them available
    // private Date creationTime;
    // private Date lastAccessed;
    private FatLfnDirectory parent;
    private FatDirEntry realEntry;

    public LfnEntry(FatLfnDirectory parent, FatDirEntry realEntry, String longName) {
        this.realEntry = realEntry;
        this.parent = parent;
        fileName = longName.trim();
        id = realEntry.getId();
    }

    public LfnEntry(FatLfnDirectory parent, Vector<?> entries, int offset, int length) {
        this.parent = parent;
        id = Integer.toString(offset);

        // this is just an old plain 8.3 entry, copy it;
        if (length == 1) {
            realEntry = (FatDirEntry) entries.get(offset);
            fileName = realEntry.getName();
            return;
        }
        // stored in reversed order
        StringBuilder name = new StringBuilder(13 * (length - 1));
        for (int i = length - 2; i >= 0; i--) {
            FatLfnDirEntry entry = (FatLfnDirEntry) entries.get(i + offset);
            name.append(entry.getSubstring());
        }
        fileName = name.toString().trim();
        realEntry = (FatDirEntry) entries.get(offset + length - 1);
    }

    public FatBasicDirEntry[] compactForm() {
        int totalEntrySize = (fileName.length() / 13) + 1; // + 1 for the real

        if ((fileName.length() % 13) != 0) // there is a remaining part
            totalEntrySize++;

        // entry
        FatBasicDirEntry[] entries = new FatBasicDirEntry[totalEntrySize];
        int j = 0;
        int checkSum = calculateCheckSum();
        for (int i = totalEntrySize - 2; i > 0; i--) {
            entries[i] =
                new FatLfnDirEntry(parent, fileName.substring(j * 13, j * 13 + 13), j + 1,
                    (byte) checkSum, false);
            j++;
        }
        entries[0] =
            new FatLfnDirEntry(parent, fileName.substring(j * 13), j + 1, (byte) checkSum, true);
        entries[totalEntrySize - 1] = realEntry;
        return entries;

    }

    private byte calculateCheckSum() {

        char[] fullName = new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
        char[] name = realEntry.getNameOnly().toCharArray();
        char[] ext = realEntry.getExt().toCharArray();
        System.arraycopy(name, 0, fullName, 0, name.length);
        System.arraycopy(ext, 0, fullName, 8, ext.length);

        byte[] dest = new byte[11];
        for (int i = 0; i < 11; i++)
            dest[i] = (byte) fullName[i];

        int sum = dest[0];
        for (int i = 1; i < 11; i++) {
            sum = dest[i] + (((sum & 1) << 7) + ((sum & 0xfe) >> 1));
        }

        return (byte) (sum & 0xff);
    }

    //@Override
    public String getId() {
        return id;
    }

    public String getName() {
        return fileName;
    }

    public Inode getParent() {
        return realEntry.getParent();
    }

    public long getCreated() {
        return realEntry.getCreated();
    }

    public long getLastModified() {
        return realEntry.getLastModified();
    }

    public long getLastAccessed() {
        return realEntry.getLastAccessed();
    }

    public boolean isFile() {
        return realEntry.isFile();
    }

    public boolean isDirectory() {
        return realEntry.isDirectory();
    }

    public void setName(String newName) {
        fileName = newName;
        realEntry.setName(parent.generateShortNameFor(newName));
    }

    public void setCreated(long created) {
        realEntry.setCreated(created);
    }

    public void setLastModified(long lastModified) {
        realEntry.setLastModified(lastModified);
    }

    public void setLastAccessed(long lastAccessed) {
        realEntry.setLastAccessed(lastAccessed);
    }

    public RegularFile getFile() throws IOException {
        return realEntry.getFile();
    }

    public Directory getDirectory() throws IOException {
        return realEntry.getDirectory();
    }

    /*public FSAccessRights getAccessRights() throws IOException {
        return realEntry.getAccessRights();
    }*/

    public boolean isValid() {
        return realEntry.isValid();
    }

    public FileSystem getFileSystem() {
        return null;//realEntry.getFileSystem();
    }

    public boolean isDeleted() {
        return realEntry.isDeleted();
    }

    public String toString() {
        return "LFN = " + fileName + " / SFN = " + realEntry.getName();
    }

    /**
     * @return Returns the realEntry.
     */
    public FatDirEntry getRealEntry() {
        return realEntry;
    }

    /**
     * @param realEntry The realEntry to set.
     */
    public void setRealEntry(FatDirEntry realEntry) {
        this.realEntry = realEntry;
    }

    /**
     * Indicate if the entry has been modified in memory (ie need to be saved)
     *
     * @return true if the entry need to be saved
     * @throws IOException
     */
    public boolean isDirty() {
        return true;
    }

    @Override
    public void setParent(Inode parent) {
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
    public int i_nlinks() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteInode() throws InodeIOException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeInode() throws InodeIOException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putInode() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void overlay(Inode newChild, String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeOverlay(Inode child) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAllOverlays() throws NoDirectoryInodeException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isOverlayed(String name) throws NoDirectoryInodeException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode lookup(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSymlink() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isWritable() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isReadable() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isExecutable() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastModified() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastAccessed() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int lastChanged() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLastModified(int time) throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLastAccessed(int time) throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] readdirNames() throws NoDirectoryInodeException, NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode getInode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rename(String oldname, Inode new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ReadOnlyMemory readWeak(int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int write(Memory mem, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int write(int pos, Memory mem, int bufoff, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int available() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getLength() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getIdentifier() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getVersion() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StatFS getStatFS() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

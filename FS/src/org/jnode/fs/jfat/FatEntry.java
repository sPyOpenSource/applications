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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import jx.fs.DirNotEmptyException;
import jx.fs.Directory;
import jx.fs.FileExistsException;
import jx.fs.FileSystem;
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
import jx.zero.Memory;
import jx.zero.ReadOnlyMemory;

public abstract class FatEntry extends FatObject implements Node
{
    //private static final Logger log = Logger.getLogger(FatEntry.class);

    private String name;
    private FatRecord record;
    private FatShortDirEntry entry;
    private FatDirectory parent;
    private FatChain chain;
    protected Vector    overlayNames;
    protected Vector    overlayInodes;
    protected boolean   i_dirty, i_released;
    
    /*
     * internal constructor
     */
    protected FatEntry(FatFileSystem fs) {
        super(fs);
    }

    public FatEntry(FatFileSystem fs, FatDirectory parent, FatRecord record) {
        this(fs);
        this.name = record.getLongName();
        this.record = record;
        this.entry = record.getShortEntry();
        this.parent = parent;
        this.chain = new FatChain(fs, entry.getStartCluster());
        overlayNames = new Vector();
        overlayInodes = new Vector();
        i_dirty = false;
    }

    private void setRoot() {
        this.name = "";
        this.record = null;
        this.entry = null;
        this.parent = null;
    }

    protected final void setRoot32(int startCluster) {
        setRoot();
        this.chain = new FatChain(getFatFileSystem(), startCluster);
    }

    @Override
    public boolean isDirty() {
        return (entry.isDirty() || chain.isDirty());
    }

    public void delete() throws IOException {
        setValid(false);

        entry.delete();
        parent.setFatDirEntry(entry);
        entry.flush();

        Vector<FatLongDirEntry> v = record.getLongEntries();

        for (int i = 0; i < v.size(); i++) {
            FatLongDirEntry l = v.get(i);
            l.delete();
            parent.setFatDirEntry(l);
            l.flush();
        }
    }

    public void freeAllClusters() throws IOException {
        getChain().freeAllClusters();
    }

    public void dumpChain(String fileName) throws FileNotFoundException, IOException {
        chain.dump(fileName);
    }

    public FatRecord getRecord() {
        return record;
    }

    public String getId() {
        return Integer.toString(entry.getIndex());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws IOException {
        this.name = name;
    }

    public String getShortName() {
        return entry.getShortName();
    }

    public boolean isShortName(byte[] shortName) {
        if (shortName.length != 11)
            throw new IllegalArgumentException("illegal shortname length: " + shortName.length);

        return Arrays.equals(shortName, entry.getName());
    }

    public int getIndex() {
        return entry.getIndex();
    }

    public long getCreated() throws IOException {
        return entry.getCreated();
    }

    public long getLastModified() throws IOException {
        return entry.getLastModified();
    }

    public long getLastAccessed() throws IOException {
        return entry.getLastAccessed();
    }

    public void setCreated(long created) throws IOException {
        entry.setCreated(created);
    }

    public void setLastModified(long lastModified) throws IOException {
        entry.setLastModified(lastModified);
    }

    public void setLastAccessed(long lastAccessed) throws IOException {
        entry.setLastAccessed(lastAccessed);
    }

    public FatShortDirEntry getEntry() {
        return entry;
    }

    protected void setEntry(FatShortDirEntry value) {
        this.entry = value;
    }

    public FatChain getChain() {
        return chain;
    }

    public int getStartCluster() {
        return getChain().getStartCluster();
    }

    @Override
    public FatDirectory getParent() {
        return parent;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public boolean isRoot() {
        return false;
    }

    public void flush() throws IOException {
        if (isDirty()) {
            if (chain.isDirty()) {
                entry.setStartCluster(chain.getStartCluster());
                chain.flush();
            }
            if (entry.isDirty()) {
                parent.setFatDirEntry(entry);
                entry.flush();
            }
        }
    }

    public Directory getDirectory() {
        throw new UnsupportedOperationException("getDirectory");
    }

    public Node getFile() {
        throw new UnsupportedOperationException("getFile");
    }

    /**
     * Gets the accessrights for this entry.
     *
     * @throws IOException
     */
    /*public FSAccessRights getAccessRights() throws IOException {
        throw new UnsupportedOperationException("not implemented yet");
    }*/

    public String getPath() {
        StringBuilder path = new StringBuilder(1024);
        FatDirectory parent = (FatDirectory) getParent();

        if (getName().length() != 0)
            path.append(getName());
        else
            path.append("\\");

        while (parent != null) {
            path.insert(0, parent.getName() + "\\");
            parent = (FatDirectory) parent.getParent();
        }

        return path.toString();
    }

    /*public String toStringValue() {
        StrWriter out = new StrWriter();

        int hashCode = System.identityHashCode(this);

        try {
            out.println("---------------------------------------");
            out.println("HashCode\t" + NumberUtils.hex(hashCode, 8));
            out.println("IsDirty\t\t" + isDirty());
            out.println("IsValid\t\t" + isValid());
            out.println("---------------------------------------");
            out.println("Name\t\t" + getName());
            out.println("ShortName\t" + getShortName());
            out.println("Path\t\t" + getPath());
            out.println("LastModified\t" + FatUtils.fTime(getLastModified()));
            out.println("isRoot\t\t" + isRoot());
            out.println("isFile\t\t" + isFile());
            out.println("isDirectory\t" + isDirectory());
            out.println("StartCluster\t" + getStartCluster());
            out.println("Chain\t\t" + getChain());
            out.print("---------------------------------------");
        } catch (IOException ex) {
            log.debug("entry error");
            out.print("entry error");
        }

        return out.toString();
    }

    public String toString() {
        StrWriter out = new StrWriter();

        out.println("*******************************************");
        out.println("FatEntry");
        out.println("*******************************************");
        out.println(toStringValue());
        out.print("*******************************************");

        return out.toString();
    }*/

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
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void decUseCount() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int i_nlinks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteNode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeNode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putNode() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void overlay(Node newChild, String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeOverlay(Node child) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException {
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
    public Node lookup(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        //if (i_released)
        //    throw new NotExistException();
        if (!isDirectory())
            throw new NoDirectoryInodeException();

        if (name.equals(".")) {
            incUseCount();
            return this;
        }
        if (name.equals("..")) {
            parent.incUseCount();
            return parent;
        }
        for (int i = 0; i < overlayNames.size(); i++) {
            if (name.equals((String)overlayNames.elementAt(i))) {
                Node inode = (Node)overlayInodes.elementAt(i);
                inode.incUseCount();
                return inode;
            }
        }

        return getNode(name);
    }

    @Override
    public boolean isSymlink() {
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
    public abstract Node getNode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException;

    @Override
    public Node mkdir(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rmdir(String name) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node create(String name, int mode) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void unlink(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node symlink(String symname, String newname) throws FileExistsException, InodeIOException, NoDirectoryInodeException, NotExistException, NotSupportedException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getSymlink() throws InodeIOException, NoSymlinkInodeException, NotExistException, NotSupportedException, PermissionException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rename(String oldname, Node new_dir, String newname) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
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
    public FileSystem getFileSystem() throws NotExistException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StatFS getStatFS() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import jx.fs.Directory;
import jx.fs.FSAttribute;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.Node;
import jx.fs.NotExistException;
import jx.fs.Permission;
import jx.fs.PermissionException;
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;

public class FatDirectory extends FatEntry
{
    public static final int MAXENTRIES = 65535; // 2^16-1; fatgen 1.03, page 33

    private final FatTable children = new FatTable();

    /**
     * The map of ID -> entry.  A linear list is used because the runtime's
     * HashMap cannot reliably insert String keys in this domain.
     */
    private final Vector<FatEntry> idEntries = new Vector<>();
    private static final int MAX_INDEX_CACHE = 256;
    private final String[] indexCacheNames = new String[MAX_INDEX_CACHE];
    private final int[] indexCacheValues = new int[MAX_INDEX_CACHE];
    private int indexCacheCount;
    private Memory entryBuffer;
    private Memory clusterBuffer;
    private int cachedCluster = -1;

    /*
     * for root directory
     */
    protected FatDirectory(FatFileSystem fs) {
        super(fs);
    }

    /*
     * from a directory record;
     */
    public FatDirectory(FatFileSystem fs, FatDirectory parent, FatRecord record) {
        super(fs, parent, record);
    }

    /*
     * initialize a new created directory
     */
    private void initialize() throws IOException {
        FatFileSystem fs = getFatFileSystem();
        FatDirectory parent = getParent();
        FatShortDirEntry entry = getEntry();
        FatChain chain = getChain();

        chain.allocateAndClear(1);

        int parentCluster = parent.isRoot() ? 0 : parent.getEntry().getStartCluster();
        int thisCluster = chain.getStartCluster();

        FatDotDirEntry dot = new FatDotDirEntry(fs, false, entry, thisCluster);
        FatDotDirEntry dotDot = new FatDotDirEntry(fs, true, entry, parentCluster);

        setFatDirEntry(dot);
        setFatDirEntry(dotDot);
    }

    /*
     * this is actually a FatDirEntry factory and not a standard read method ...
     * but how would you call it?
     */
    public synchronized FatDirEntry getFatDirEntry(int index, boolean allowDeleted) throws IOException {
        final int clusterSize = getFatFileSystem().getFat().getClusterSize();
        final int byteOffset = index * FatDirEntry.LENGTH;
        final int clusterIndex = byteOffset / clusterSize;

        if (clusterBuffer == null || clusterBuffer.size() < clusterSize) {
            MemoryManager mm = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
            clusterBuffer = mm.alloc(clusterSize);
            cachedCluster = -1;
        }
        if (entryBuffer == null) {
            MemoryManager mm = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
            entryBuffer = mm.alloc(FatDirEntry.LENGTH);
        }
        if (cachedCluster != clusterIndex) {
            getChain().read(clusterIndex * clusterSize, clusterBuffer, clusterSize);
            cachedCluster = clusterIndex;
        }
        entryBuffer.copyFromMemory(clusterBuffer, byteOffset % clusterSize, 0, FatDirEntry.LENGTH);

        FatMarshal entry = new FatMarshal(entryBuffer);
        return createDirEntry(entry, index, allowDeleted);
    }

    /**
     * Creates a new FAT directory entry.
     *
     * @param entry the FAT entry buffer.
     * @param index the index of the entry.
     * @param allowDeleted {@code true} to allow deleted entries to be returned, {@code false} to only allow live
     *     entries.
     * @return the FAT directory entry.
     * @throws java.io.IOException
     */
    public FatDirEntry createDirEntry(FatMarshal entry, int index, boolean allowDeleted)
        throws IOException {
        int flag;
        FatAttr attr;

        flag = entry.getUInt8(0);
        attr = new FatAttr(entry.getUInt8(11));
        boolean free = flag == FatDirEntry.FREE;

        switch (flag) {
            case FatDirEntry.EOD:
                return new FatDirEntry(getFatFileSystem(), entry, index, flag);
            case FatDirEntry.FREE:
                if (!allowDeleted) {
                    return new FatDirEntry(getFatFileSystem(), entry, index, flag);
                } else {
                    // Fall through...
                    break;
                }
            case FatDirEntry.INVALID:
                throw new IOException("Invalid entry for index: " + index);
        }

        FatDirEntry fatDirEntry;
        // 0xffffffff is the end of long file name marker
        if (attr.isLong() || entry.getUInt32(28) == 0xffffffff) {
            fatDirEntry = createLongDirEntry(entry, index);
        } else {
            fatDirEntry = createShortDirEntry(entry, index);
        }

        if (free) {
            // Still mark deleted entries as deleted.
            fatDirEntry.setFreeDirEntry(true);
        }

        return fatDirEntry;
    }

    /**
     * Creates a new short directory entry.
     *
     * @param entry the FAT marshal entry.
     * @param index the index of the entry.
     * @return the short directory entry.
     */
    protected FatDirEntry createShortDirEntry(FatMarshal entry, int index) {
        return new FatShortDirEntry(getFatFileSystem(), entry, index);
    }

    /**
     * Creates a new long directory entry.
     *
     * @param entry the FAT entry buffer.
     * @param index the index of the entry.
     * @return the long directory entry.
     * @throws java.io.IOException
     */
    protected FatDirEntry createLongDirEntry(FatMarshal entry, int index) throws IOException {
        return new FatLongDirEntry(getFatFileSystem(), entry, index);
    }

    /*
     * this instead is a "write" method: it needs a "created" entry
     */
    public void setFatDirEntry(FatDirEntry entry) throws IOException {
        final byte[] bytes = new byte[FatDirEntry.LENGTH];
        entry.entry.getBytes(0, FatDirEntry.LENGTH, 0, bytes);

        MemoryManager mm = (MemoryManager) InitialNaming.getInitialNaming().lookup("MemoryManager");
        Memory mem = mm.alloc(FatDirEntry.LENGTH);
        mem.copyFromByteArray(bytes, 0, 0, bytes.length);

        final FatFileSystem fs = getFatFileSystem();
        cachedCluster = -1;
        if (isRoot() && !fs.getFat().isFat32()) {
            BootSector bs = fs.getBootSector();
            final int bytesPerSector = bs.getBytesPerSector();
            // For FAT12/16 getFirstDataSector() is the start of the fixed root
            // directory, not the first data cluster.
            final int rootStartSector = bs.getFirstDataSector();
            final int byteOffset = entry.getIndex() * FatDirEntry.LENGTH;
            final int sector = rootStartSector + byteOffset / bytesPerSector;
            final int sectorOffset = byteOffset % bytesPerSector;

            Memory sectorBuf = mm.alloc(bytesPerSector);
            fs.getApi().readSectors(sector, 1, sectorBuf, true);
            sectorBuf.copyFromByteArray(bytes, 0, sectorOffset, bytes.length);
            fs.getApi().writeSectors(sector, 1, sectorBuf, true);
        } else {
            getChain().write(entry.getIndex() * FatDirEntry.LENGTH, mem, FatDirEntry.LENGTH);
        }
    }

    public FatDirEntry[] getFatFreeEntries(int n) throws IOException {
        int i = 0;
        int index = 0;
        FatDirEntry entry = null;
        FatDirEntry[] entries = new FatDirEntry[n];

        while (i < n) {
            try {
                entry = getFatDirEntry(index, false);
                index++;
            } catch (NoSuchElementException ex) {
                if (index > MAXENTRIES)
                    throw new IOException("Directory is full");
                getChain().allocateAndClear(1);
                // restart the search, fixes infinite loop
                i = 0;
                index = 0;
                continue;
            } catch (IOException ex) {
                // Reading past the end of the directory chain: allocate another
                // cluster and restart the scan from the beginning of the new chain.
                getChain().allocateAndClear(1);
                i = 0;
                index = 0;
                continue;
            }

            if (entry.isFreeDirEntry() || entry.isLastDirEntry()) {
                entries[i] = entry;
                i++;
            } else {
                i = 0;
            }
        }

        return entries;
    }

    public String getDirectoryId() {
        return Integer.toString(getStartCluster());
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public Directory getDirectory() {
        return null;//this;
    }

    protected FatTable getVisitedChildren() {
        return children;
    }

    public Iterator<Node> iterator() {
        return null;//new FatEntriesIterator(children, this, false);
    }

    /**
     * Creates a new directory entry iterator.
     *
     * @param includeDeleted {@code true} if deleted files and directory entries should be returned, {@code false}
     *                       otherwise.
     * @return the iterator.
     */
    public Iterator<Node> createIterator(boolean includeDeleted) {
        return null;//new FatEntriesIterator(new FatTable(), this, includeDeleted);
    }

    /*
     * used from a FatRootDirectory looking for its label
     */
    protected void scanDirectory() {
        FatEntriesFactory f = new FatEntriesFactory(this, false);

        while (f.hasNextEntry()){
            f.createNextEntry();
        }
    }

    private int findCachedIndex(String name) {
        if (name == null)
            return -1;
        for (int i = 0; i < indexCacheCount; i++) {
            if (indexCacheNames[i] != null && FatUtils.compareIgnoreCase(indexCacheNames[i], name))
                return indexCacheValues[i];
        }
        return -1;
    }

    void cacheEntry(String name, int startIndex) {
        if (name == null || name.length() == 0)
            return;
        for (int i = 0; i < indexCacheCount; i++) {
            if (indexCacheNames[i] != null && FatUtils.compareIgnoreCase(indexCacheNames[i], name)) {
                indexCacheValues[i] = startIndex;
                return;
            }
        }
        if (indexCacheCount < MAX_INDEX_CACHE) {
            indexCacheNames[indexCacheCount] = name;
            indexCacheValues[indexCacheCount] = startIndex;
            indexCacheCount++;
        }
    }

    private FatEntry getIdEntry(String id) {
        for (int i = 0; i < idEntries.size(); i++) {
            FatEntry entry = idEntries.elementAt(i);
            if (entry.getId().equals(id))
                return entry;
        }
        return null;
    }

    private void putIdEntry(String id, FatEntry entry) {
        if (getIdEntry(id) == null)
            idEntries.addElement(entry);
    }

    private void removeIdEntry(String id) {
        for (int i = 0; i < idEntries.size(); i++) {
            if (idEntries.elementAt(i).getId().equals(id)) {
                idEntries.removeElementAt(i);
                return;
            }
        }
    }

    public synchronized Node getEntry(String name) {
        FatEntry child = children.get(name);

        if (child == null) {
            int startIndex = findCachedIndex(name);
            if (startIndex >= 0) {
                FatEntriesFactory cached = new FatEntriesFactory(this, false, startIndex);
                if (cached.hasNextEntry()) {
                    child = cached.createNextEntry();
                    if (FatUtils.compareIgnoreCase(child.getName(), name)) {
                        children.put(child);
                        return child;
                    }
                    child = null;
                }
            }
        }

        if (child == null) {
            FatEntriesFactory f = new FatEntriesFactory(this, false);

            while (f.hasNextEntry()) {
                FatEntry entry = f.createNextEntry();
                if (FatUtils.compareIgnoreCase(entry.getName(), name)) {
                    child = children.put(entry);
                    break;
                }
            }
        }

        return child;
    }

    public Node getEntryById(String id) throws IOException {
        FatEntry child = getIdEntry(id);

        if (child == null) {
            FatEntriesFactory f = new FatEntriesFactory(this, true);

            while (f.hasNextEntry()) {
                FatEntry entry = f.createNextEntry();
                putIdEntry(entry.getId(), entry);
            }

            return getIdEntry(id);
        }

        return child;
    }

    public FatEntry getEntryByShortName(byte[] shortName) {
        FatEntry child = null;
        FatEntriesFactory f = new FatEntriesFactory(this, false);

        while (f.hasNextEntry()) {
            FatEntry entry = f.createNextEntry();
            if (entry.isShortName(shortName)) {
                child = entry;
                break;
            }
        }

        return child;
    }

    public FatEntry getEntryByName(String name) {
        FatEntry child = null;
        int startIndex = findCachedIndex(name);
        if (startIndex >= 0) {
            FatEntriesFactory cached = new FatEntriesFactory(this, false, startIndex);
            if (cached.hasNextEntry()) {
                child = cached.createNextEntry();
                if (FatUtils.compareIgnoreCase(child.getName(), name))
                    return child;
            }
        }

        FatEntriesFactory f = new FatEntriesFactory(this, false);

        while (f.hasNextEntry()) {
            FatEntry entry = f.createNextEntry();
            if (FatUtils.compareIgnoreCase(entry.getName(), name)) {
                child = entry;
                break;
            }
        }

        return child;
    }

    public boolean collide(byte[] shortName) {
        return !(getEntryByShortName(shortName) == null);
    }

    public boolean collide(String name) {
        return !(getEntryByName(name) == null);
    }

    public boolean isEmpty() {
        if (isRoot())
            return false;
        Iterator<Node> i = iterator();
        while (i.hasNext()) {
            String name = null;//i.next().getName();
            if (!name.equals(".") && !name.equals(".."))
                return false;
        }
        return true;
    }

    public synchronized Node addFile(String name) throws IOException {
        FatName fatName = new FatName(this, name);
        if (collide(fatName.getLongName()))
            throw new IOException("File [" + fatName.getLongName() + "] already exists");
        FatRecord record = new FatRecord(this, fatName);
        record.getShortEntry().setArchive();
        FatFile file = new FatFile(getFatFileSystem(), this, record);
        file.flush();

        FatEntry entry = children.put(file);
        putIdEntry(entry.getId(), entry);
        int startIndex = entry.getIndex();
        java.util.Vector<FatLongDirEntry> longEntries = file.getRecord().getLongEntries();
        if (longEntries.size() > 0)
            startIndex = ((FatLongDirEntry) longEntries.elementAt(0)).getIndex();
        cacheEntry(name, startIndex);
        return entry;
    }

    @Override
    public Node create(String name, int mode) throws IOException {
        FatEntry existing = (FatEntry) getEntryByName(name);
        if (existing != null) {
            if (!existing.isFile())
                throw new IOException("Entry [" + name + "] is a directory");
            ((FatFile) existing).truncate();
            return existing;
        }
        return addFile(name);
    }

    @Override
    public void unlink(String name) throws IOException {
        FatEntry entry = (FatEntry) getEntryByName(name);
        if (entry == null)
            throw new FileNotFoundException(name);

        if (entry.isFile())
            ((FatFile) entry).truncate();
        else
            entry.freeAllClusters();
        entry.delete();
        entry.flush();

        try {
            children.remove(entry);
        } catch (IllegalArgumentException ignored) {
        }
        removeIdEntry(entry.getId());
        for (int i = 0; i < indexCacheCount; i++) {
            if (indexCacheNames[i] != null && FatUtils.compareIgnoreCase(indexCacheNames[i], name)) {
                indexCacheNames[i] = null;
                indexCacheValues[i] = -1;
            }
        }
    }

    public synchronized Node addDirectory(String name) throws IOException {
        FatFileSystem fs = getFatFileSystem();
        FatName fatName = new FatName(this, name);
        if (collide(fatName.getLongName()))
            throw new IOException("File [" + fatName.getLongName() + "] already exists");
        FatRecord record = new FatRecord(this, fatName);
        record.getShortEntry().setDirectory();
        FatDirectory dir = new FatDirectory(fs, this, record);
        dir.initialize();
        dir.flush();

        FatEntry entry = children.put(dir);
        putIdEntry(entry.getId(), entry);
        return entry;
    }

    public synchronized void remove(String name) throws IOException {
        FatEntry entry = (FatEntry) getEntry(name);
        if (entry == null)
            throw new FileNotFoundException(name);
        if (entry.isFile()) {
            FatFile file = (FatFile) entry.getFile();
            children.remove(entry);
            file.delete();
            file.freeAllClusters();
            file.flush();
        } else {
            FatDirectory dir = (FatDirectory) entry;
            if (!dir.isEmpty())
                throw new UnsupportedOperationException("Directory is not empty: " + name);
            children.remove(entry);
            dir.delete();
            dir.freeAllClusters();
            dir.flush();
        }

        removeIdEntry(entry.getId());
    }

    @Override
    public String toString() {
        return String.format("FatDirectory [%s] index:%d", getName(), getIndex());
    }

    /*public String toDebugString() {
        StrWriter out = new StrWriter();
        out.println("*******************************************");
        out.println("FatDirectory");
        out.println("*******************************************");
        out.println("Index\t\t" + getIndex());
        out.println(toStringValue());
        out.println("Visited\t\t" + getVisitedChildren());
        out.print("*******************************************");
        return out.toString();
    }*/

    @Override
    public Node getNode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	FatEntry inode;
	
	if (i_released)
	    throw new NotExistException();
	
	inode = getEntryByName(name);

	if (inode != null) {
	    return inode;
	}
	
	return inode;
    }

    @Override
    public Permission getPermission() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public FSAttribute getAttribute() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

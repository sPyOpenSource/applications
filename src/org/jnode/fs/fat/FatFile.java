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
import jx.bio.BlockIO;
import jx.zero.Memory;

/**
 * A File instance is the in-memory representation of a single file (chain of
 * clusters).
 * 
 * @author epr
 */
public class FatFile extends FatObject {
    private long startCluster;
    private long length;
    private FatDirectory dir;
    private final int clusterSize;
    private final boolean isDir;
    private final FatDirEntry myEntry;

    public FatFile(FatFileSystem fs, FatDirEntry myEntry, long startCluster, long length, boolean isDir) {
        super(fs);
        this.myEntry = myEntry;
        this.startCluster = startCluster;
        this.length = length;
        this.clusterSize = fs.getClusterSize();
        this.isDir = isDir;
    }

    public synchronized void read(long fileOffset, Memory destBuf) throws IOException {
        int len = 0;//destBuf.remaining();

        final long max = (isDir) ? getLengthOnDisk() : getLength();
        if (fileOffset + len > max) {
            throw new IOException("Cannot read beyond the EOF");
        }

        final FatFileSystem fs = getFatFileSystem();
        final long[] chain = fs.getFat().getChain(startCluster);
        final BlockIO api = fs.getApi();

        int chainIdx = (int) (fileOffset / clusterSize);
        if (fileOffset % clusterSize != 0) {
            int clusOfs = (int) (fileOffset % clusterSize);
            int size = Math.min(len, (int) (clusterSize - (fileOffset % clusterSize) - 1));
            //destBuf.limit(destBuf.position() + size);
            api.readSectors((int)getDevOffset(chain[chainIdx], clusOfs), 1, destBuf, true);
            fileOffset += size;
            len -= size;
            chainIdx++;
        }
        while (len > 0) {
            int size = Math.min(clusterSize, len);
            //destBuf.limit(destBuf.position() + size);
            api.readSectors((int)getDevOffset(chain[chainIdx], 0), 1, destBuf, true);
            len -= size;
            chainIdx++;
        }
    }

    public synchronized void write(long fileOffset, Memory srcBuf) throws IOException {
        int len = 0;//srcBuf.remaining();

        /*if (getFileSystem().isReadOnly()) {
            throw new ReadOnlyFileSystemException("write in readonly filesystem");
        }*/

        final long max = (isDir) ? getLengthOnDisk() : getLength();
        if (fileOffset > max) {
            throw new IOException("Cannot write beyond the EOF");
        }

        if (fileOffset + len > max) { // this is too short increase the size
                                        // of the file
            setLength(fileOffset + len);
        }

        final FatFileSystem fs = getFatFileSystem();
        final long[] chain = fs.getFat().getChain(getStartCluster());
        final BlockIO api = fs.getApi();

        int chainIdx = (int) (fileOffset / clusterSize);
        if (fileOffset % clusterSize != 0) {
            int clusOfs = (int) (fileOffset % clusterSize);
            int size = Math.min(len, (int) (clusterSize - (fileOffset % clusterSize) - 1));
            //srcBuf.limit(srcBuf.position() + size);
            api.writeSectors((int)getDevOffset(chain[chainIdx], clusOfs), srcBuf.size(), srcBuf, true);
            fileOffset += size;
            len -= size;
            chainIdx++;
        }
        while (len > 0) {
            int size = Math.min(clusterSize, len);
            //srcBuf.limit(srcBuf.position() + size);
            api.writeSectors((int)getDevOffset(chain[chainIdx], 0), srcBuf.size(), srcBuf, true);
            len -= size;
            chainIdx++;
        }
    }

    /**
     * Sets the length.
     * 
     * @param length The length to set
     * @return 
     * @throws java.io.IOException
     */
    public synchronized int setLength(long length) throws IOException {

        /*if (getFileSystem().isReadOnly()) {
            throw new ReadOnlyFileSystemException("setLength in readonly filesystem");
        }*/

        if (this.length == length) {
            // Do nothing
            return 0;
        }

        final FatFileSystem fs = getFatFileSystem();
        final org.jnode.fs.jfat.Fat fat = fs.getFat();
        final int nrClusters = (int) ((length + clusterSize - 1) / clusterSize);

        if (this.length == 0) {
            final long[] chain = fat.allocNew(nrClusters);
            this.startCluster = chain[0];
            this.myEntry.setStartCluster((int) startCluster);
        } else {
            final long[] chain = fs.getFat().getChain(startCluster);

            if (nrClusters != chain.length) {
                if (nrClusters > chain.length) {
                    // Grow
                    int count = nrClusters - chain.length;
                    while (count > 0) {
                        fat.allocAppend(getStartCluster());
                        count--;
                    }
                } else {
                    // Shrink
                    fat.setEof(chain[nrClusters - 1]);
                    for (int i = nrClusters; i < chain.length; i++) {
                        fat.setFree(chain[i]);
                    }
                }
            }
        }

        this.length = length;
        this.myEntry.updateLength(length);
        return 0;
    }

    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return length;
    }

    /**
     * Gets the size this file occupies on disk
     * 
     * @return long
     */
    public long getLengthOnDisk() {
        if (this.length == 0) {
            return 0;
        } else {
            final FatFileSystem fs = getFatFileSystem();
            final long[] chain = fs.getFat().getChain(getStartCluster());
            return ((long) chain.length) * fs.getClusterSize();
        }
    }

    /**
     * Returns the startCluster.
     * 
     * @return long
     */
    public long getStartCluster() {
        return startCluster;
    }

    /**
     * Gets the directory contained in this file.
     * 
     * @return Directory
     * @throws java.io.IOException
     */
    public synchronized FatDirectory getDirectory() throws IOException {
        if (dir == null) {
            final FatFileSystem fs = getFatFileSystem();
            dir = new FatLfnDirectory(fs, this);
        }
        return dir;
    }

    /**
     * Calculates the device offset (0-based) for the given cluster and offset
     * within the cluster.
     * @param cluster
     * @param clusterOffset
     * @return long
     */
    protected long getDevOffset(long cluster, int clusterOffset) {
        final FatFileSystem fs = getFatFileSystem();
        final long filesOffset = 0;//FatUtils.getFilesOffset(fs.getBootSector());
        return 0;//filesOffset + clusterOffset + ((cluster - FatUtils.FIRST_CLUSTER) * clusterSize);
    }

    /**
     * Flush any changes in this file to persistent storage
     * @throws IOException
     */
    public void flush() throws IOException {
        if (dir != null) {
            dir.flush();
        }
    }
}

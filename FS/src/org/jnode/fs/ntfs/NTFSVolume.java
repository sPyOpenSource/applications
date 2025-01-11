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
import java.nio.ByteBuffer;
import jx.devices.bio.BlockIO;
import jx.zero.Memory;
import jx.zero.MemoryManager;

/**
 * @author Chira
 */
public class NTFSVolume {
MemoryManager MemManager;

    //private static final Logger log = Logger.getLogger(NTFSVolume.class);

    public static final byte LONG_FILE_NAMES = 0x01;

    public static final byte DOS_8_3 = 0x02;

    private final byte currentNameSpace = LONG_FILE_NAMES;

    private final BlockIO api;

    // local chache for faster access
    private final int clusterSize;

    private final BootSector bootRecord;

    private MasterFileTable mftFileRecord;

    private FileRecord rootDirectory;

    /**
     * Initialize this instance.
     */
    public NTFSVolume(BlockIO api) throws IOException {
        // I hope this is enaugh..should be
        this.api = api;

        // Read the boot sector
        final Memory buffer = MemManager.alloc(512);
        api.readSectors(0, 1, buffer, true);
        this.bootRecord = new BootSector(buffer);
        this.clusterSize = bootRecord.getClusterSize();
    }

    /**
     * @return Returns the bootRecord.
     */
    public final BootSector getBootRecord() {
        return bootRecord;
    }

    /**
     * Read a single cluster.
     *
     * @param cluster
     */
    public void readCluster(int cluster, Memory dst, int dstOffset) throws IOException {
        final int clusterSize = getClusterSize();
        final int clusterOffset = cluster * clusterSize;
        //log.debug("readCluster(" + cluster + ") " + (readClusterCount++));
        //api.readSectors(clusterOffset, dst, dstOffset, clusterSize);
    }

    private int readClusterCount;
    private int readClustersCount;

    /**
     * Read a number of clusters.
     *
     * @param firstCluster
     * @param nrClusters   The number of clusters to read.
     * @param dst          Must have space for (nrClusters * getClusterSize())
     * @param dstOffset
     * @throws IOException
     */
    public void readClusters(int firstCluster, Memory dst, int dstOffset, int nrClusters) throws IOException {
        //log.debug("readClusters(" + firstCluster + ", " + nrClusters + ") " + (readClustersCount++));
        final int clusterSize = getClusterSize();
        final int clusterOffset = firstCluster * clusterSize;
        //api.readSectors(clusterOffset, dst, dstOffset, nrClusters * clusterSize);
    }

    /**
     * Gets the size of a cluster.
     *
     * @return the size
     */
    public int getClusterSize() {
        return clusterSize;
    }

    /**
     * Gets the MFT.
     *
     * @return Returns the mTFRecord.
     */
    public MasterFileTable getMFT() throws IOException {
        if (mftFileRecord == null) {
            final BootSector bootRecord = getBootRecord();
            final int bytesPerFileRecord = bootRecord.getFileRecordSize();
            final int clusterSize = getClusterSize();

            final int nrClusters;
            if (bytesPerFileRecord < clusterSize) {
                nrClusters = 1;
            } else {
                nrClusters = (bytesPerFileRecord + clusterSize - 1) / clusterSize;
            }
            final Memory data = MemManager.alloc(nrClusters * clusterSize);
            readClusters((int)bootRecord.getMftLcn(), data, 0, nrClusters);
            mftFileRecord = new MasterFileTable(this, data, 0);
            mftFileRecord.checkIfValid();
        }
        return mftFileRecord;

    }

    /**
     * Gets the root directory on this volume.
     *
     * @return the root directory record
     * @throws IOException
     */
    public FileRecord getRootDirectory() throws IOException {
        if (rootDirectory == null) {
            // Read the root directory
            final MasterFileTable mft = getMFT();
            rootDirectory = mft.getRecord(MasterFileTable.SystemFiles.ROOT);
            //log.info("getRootDirectory: " + rootDirectory.getFileName());
        }
        return rootDirectory;
    }

    /**
     * @return Returns the currentNameSpace.
     */
    public byte getCurrentNameSpace() {
        return currentNameSpace;
    }
}

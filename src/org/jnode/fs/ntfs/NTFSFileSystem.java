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
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.FileSystemException;

import jx.bio.BlockIO;
import jx.fs.FSException;
import jx.fs.Inode;
import jx.fs.buffercache.BufferCache;
import jx.zero.Clock;

import org.jnode.fs.ntfs.attribute.NTFSAttribute;
import org.jnode.fs.ntfs.attribute.NTFSResidentAttribute;

/**
 * NTFS filesystem implementation.
 *
 * @author Chira
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class NTFSFileSystem implements jx.fs.FileSystem {

    private final NTFSVolume volume;
    private FSEntry root;

    /**
     * @see org.jnode.fs.FileSystem#getDevice()
     */
    public NTFSFileSystem(BlockIO device, boolean readOnly, NTFSFileSystemType type) throws FileSystemException {
        super(device, readOnly, type);

        try {
            // initialize the NTFS volume
            volume = new NTFSVolume(device);
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage());
        }
    }

    /**
     * @see org.jnode.fs.FileSystem#getRootEntry()
     */
    public FSEntry getRootEntry() throws IOException {
        if (root == null) {
            root = new NTFSEntry(this, getNTFSVolume().getRootDirectory(), -1);
        }
        return root;
    }

    /**
     * @return Returns the volume.
     */
    public NTFSVolume getNTFSVolume() {
        return this.volume;
    }

    public String getVolumeName() throws IOException {
        NTFSEntry entry = new NTFSEntry(this, getNTFSVolume().getMFT().getRecord(MasterFileTable.SystemFiles.VOLUME),
            MasterFileTable.SystemFiles.ROOT);

        NTFSAttribute attribute = entry.getFileRecord().findAttributeByType(NTFSAttribute.Types.VOLUME_NAME);

        if (attribute instanceof NTFSResidentAttribute) {
            NTFSResidentAttribute residentAttribute = (NTFSResidentAttribute) attribute;
            byte[] nameBuffer = new byte[residentAttribute.getAttributeLength()];

            residentAttribute.getData(residentAttribute.getAttributeOffset(), nameBuffer, 0, nameBuffer.length);

            try {
                // XXX: For Java 6, should use the version that accepts a Charset.
                return new String(nameBuffer, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-16LE charset missing from JRE", e);
            }
        }

        return "";
    }

    /**
     * Gets the volume's ID.
     *
     * @return the volume ID.
     * @throws IOException if an error occurs.
     */
    public byte[] getVolumeId() throws IOException {
        NTFSEntry entry = (NTFSEntry) getRootEntry().getDirectory().getEntry("$Volume");
        if (entry == null) {
            return null;
        }

        NTFSAttribute attribute = entry.getFileRecord().findAttributeByType(NTFSAttribute.Types.OBJECT_ID);

        if (attribute instanceof NTFSResidentAttribute) {
            NTFSResidentAttribute residentAttribute = (NTFSResidentAttribute) attribute;
            byte[] idBuffer = new byte[residentAttribute.getAttributeLength()];

            residentAttribute.getData(residentAttribute.getAttributeOffset(), idBuffer, 0, idBuffer.length);
            return idBuffer;
        }

        return null;
    }

    /**
     * Flush all data.
     */
    public void flush() throws IOException {
        // TODO Auto-generated method stub
    }

    /**
     *
     */
    protected FSFile createFile(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     */
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     */
    protected NTFSEntry createRootEntry() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public long getFreeSpace() throws IOException {
        FileRecord bitmapRecord = getNTFSVolume().getMFT().getRecord(MasterFileTable.SystemFiles.BITMAP);

        int bitmapSize = (int) bitmapRecord.getAttributeTotalSize(NTFSAttribute.Types.DATA, null);
        byte[] buffer = new byte[bitmapSize];
        bitmapRecord.readData(0, buffer, 0, buffer.length);

        int usedBlocks = 0;

        for (byte b : buffer) {
            for (int i = 0; i < 8; i++) {
                if ((b & 0x1) != 0) {
                    usedBlocks++;
                }

                b >>= 1;
            }
        }

        long usedSpace = (long) usedBlocks * getNTFSVolume().getClusterSize();

        return getTotalSpace() - usedSpace;
    }

    public long getTotalSpace() throws IOException {
        FileRecord bitmapRecord = getNTFSVolume().getMFT().getRecord(MasterFileTable.SystemFiles.BITMAP);
        long bitmapSize = bitmapRecord.getFileNameAttribute().getRealSize();
        return bitmapSize * 8 * getNTFSVolume().getClusterSize();
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }

    @Override
    public void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String name() {
        try {
            return getVolumeName();
        } catch (IOException ex) {
            Logger.getLogger(NTFSFileSystem.class.getName()).log(Level.SEVERE, null, ex);
            return "no name";
        }
    }

    @Override
    public Inode getRootInode() {
        try {
            return getRootEntry();
        } catch (IOException ex) {
            Logger.getLogger(NTFSFileSystem.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public void init(boolean read_only) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void release() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void build(String name, int blocksize) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void check() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Inode getInode(int identifier) throws FSException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDeviceID() {
        try {
            return new Integer(getVolumeId()[0]);
        } catch (IOException ex) {
            Logger.getLogger(NTFSFileSystem.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public String read(String aitxt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

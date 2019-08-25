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
import java.util.logging.Level;
import java.util.logging.Logger;
import javafs.InodeCache;
import javafs.Tools;
import jx.bio.BlockIO;
import jx.fs.Directory;
import jx.fs.FSException;
import jx.fs.Inode;
import jx.fs.RegularFile;
import jx.fs.buffercache.BufferCache;
import jx.zero.Clock;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import org.jnode.fs.jfat.BootSector;
import org.jnode.fs.jfat.Fat;
import org.jnode.fs.jfat.FatRootDirectory;

/**
 * @author epr
 */
public class FatFileSystem implements jx.fs.FileSystem {
    private Fat fat;
    private BlockIO drive;
    private boolean     inited = false;
    private BufferCache bufferCache;
    private final InodeCache  inodeCache;
    private Tools       tools;
    private Clock       clock;
    private final Integer     deviceID;
    
    
    /**
     * Constructor for FatFileSystem in specified readOnly mode
     * @param readOnly
     */
    public FatFileSystem(boolean readOnly) //throws FileSystemException 
    {
        //super(readOnly, type); // false = read/write mode
        
        //try {
            drive = (BlockIO)InitialNaming.getInitialNaming().lookup("BIOFS_RW");
        try {
            fat = Fat.create(getApi());
        } catch (IOException ex) {
            //Logger.getLogger(FatFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        inodeCache = new InodeCache();
	deviceID = new Integer(1);
            
                        
//            if (!bs.isaValidBootSector()) throw new FileSystemException(
//                "Can't mount this partition: Invalid BootSector");


            /*FatType bitSize;

            if (bs.getMediumDescriptor() == 0xf8) {
                bitSize = FatType.FAT16;
            } else {
                bitSize = FatType.FAT12;
            }*/

            /*for (int i = 0; i < fats.length; i++) {
                Fat fat = new Fat(bitSize, bs.getMediumDescriptor(), bs.getSectorsPerFat(), bs.getBytesPerSector());
                fats[i] = fat;
                fat.read((BlockIO)naming.lookup("BIOFS_RW"), FatUtils.getFatOffset(bs, i));
            }

            for (int i = 1; i < fats.length; i++) {
                if (!fats[0].equals(fats[i])) {
                    System.out.println("FAT " + i + " differs from FAT 0");
                }
            }*/
        //} catch (Exception e) { // something bad happened in the FAT boot
            // sector... just ignore this FS
            //throw new FileSystemException(e);
        //}
    }

    public FatFileSystem(BlockIO bio) {
        drive = bio;
        try {
            fat = Fat.create(bio);
        } catch (IOException ex) {
            //Logger.getLogger(FatFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        inodeCache = new InodeCache();
	deviceID = new Integer(1);
        //int offset = FatUtils.getRootDirOffset(bs);
        //Debug.out.println(offset);
        //Memory data = memoryManager.allocAligned(16 * 32, 8);
        //Debug.out.println(bs.getNrRootDirEntries());
        //drive.readSectors(7, 1, data, true);
    }

    /**
     * Gets the file for the given entry.
     *
     * @param entry
     * @return 
     */
    public synchronized FatFile getFile(FatDirEntry entry) {

        /*FatFile file = files.get(entry);
        if (file == null) {
            file = new FatFile(this, entry, entry.getStartCluster(), entry.getLength(), entry.isDirectory());
            files.put(entry, file);
        }
        return file;*/
        return null;
    }

    public int getClusterSize() {
        return fat.getClusterSize();
    }

    /**
     * Returns the fat.
     *
     * @return Fat
     */
    public Fat getFat() {
        return fat;
    }

    /**
     * Returns the bootsector.
     *
     * @return BootSector
     */
    public BootSector getBootSector() {
        return fat.getBootSector();
    }
    
    /**
     *
     * @param entry
     * @return 
     * @throws java.io.IOException
     */
    protected RegularFile createFile(Inode entry) throws IOException {

        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     * @param entry
     * @return 
     * @throws java.io.IOException
     */
    protected Directory createDirectory(Inode entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public long getFreeSpace() {
        // TODO implement me
        return -1;
    }

    public long getTotalSpace() {
        // TODO implement me
        return -1;
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }

    public BlockIO getApi() {
        return drive;
    }

    @Override
    public void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String name() {
        return "FAT";
    }

    @Override
    public Inode getRootInode() {
        try {
            return new FatRootDirectory(this);
        } catch (IOException ex) {
            //Logger.getLogger(FatFileSystem.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public void init(boolean read_only) {
        if (inited)
	    return;
	
	inited = true;
	Debug.out.println("INIT JavaFS");
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
    public Integer getDeviceID() {
        return deviceID; /* FIXME */
    }
}

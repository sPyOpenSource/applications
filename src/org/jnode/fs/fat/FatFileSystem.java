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
import jx.fs.Directory;
import jx.fs.FSException;
import jx.fs.FileSystem;
import jx.fs.Inode;
import jx.fs.RegularFile;
import jx.fs.buffercache.BufferCache;
import jx.zero.Clock;
import jx.zero.InitialNaming;
import org.jnode.fs.jfat.BootSector;
import org.jnode.fs.jfat.Fat;
import org.jnode.fs.jfat.FatRootDirectory;
//import org.jnode.fs.FileSystemException;
//import org.jnode.fs.spi.AbstractFileSystem;

/**
 * @author epr
 */
public class FatFileSystem implements FileSystem {
    private Fat fat;
    private BlockIO drive;
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
        //int offset = FatUtils.getRootDirOffset(bs);
        //Debug.out.println(offset);
        //Memory data = memoryManager.allocAligned(16 * 32, 8);
        //Debug.out.println(bs.getNrRootDirEntries());
        //drive.readSectors(7, 1, data, true);
    }
    
    public FatRootDirectory getRootEntry() throws IOException {
        //if (isClosed())
          //  throw new IOException("FileSystem is closed");

        //if (rootEntry == null) {
            FatRootDirectory rootEntry = createRootEntry();
        //}
        return rootEntry;
    }
    
    @Override
    public void build(String name, int blocksize){}
    
    @Override
    public void init(boolean x){}
    
    @Override
    public void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock){}
    
    @Override
    public Integer getDeviceID(){
        return null;
    }
    /**
     * Flush all changed structures to the device.
     *
     * @throws IOException
     */
    @Override
    public void release()// throws IOException 
    {

        /*final BlockIO api = (BlockIO)naming.lookup("BIOFS_RW");

        if (bs.isDirty()) {
            bs.write(api);
        }

        for (FatFile f : files.values()) {
            f.flush();
        }

        if (fat.isDirty()) {
            for (int i = 0; i < bs.getNrFats(); i++) {
                fat.write(api, FatUtils.getFatOffset(bs, i));
            }
        }

        if (rootDir.isDirty()) {
            rootDir.flush();
        }*/

    }

    /**
     * Gets the root entry of this filesystem. This is usually a directory, but this is not required.
     * @return 
     */
    protected FatRootDirectory createRootEntry() throws IOException {
        return new FatRootDirectory(this);
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
    
    @Override
    public Inode getInode(int identifier) throws FSException{
        return null;
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

    @Override
    public String name() //throws IOException 
    {
        return ""; // TODO implement me
    }
    
    @Override
    public void check(){}

    public BlockIO getApi() {
        return drive;
    }

    @Override
    public Inode getRootInode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

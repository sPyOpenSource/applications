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

import java.io.IOException;
import jx.devices.bio.BlockIO;
import jx.zero.Clock;
import jx.zero.Debug;
import jx.zero.InitialNaming;

import jx.fs.javafs.InodeCache;
import jx.fs.javafs.Tools;
import jx.fs.Directory;
import jx.fs.FSException;
import jx.fs.Node;
import jx.fs.RegularFile;
import jx.fs.buffer.BufferCache;


/**
 * @author epr
 */
public class FatFileSystem implements jx.fs.FileSystem {
    private Fat fat;
    private BlockIO drive;
    private boolean inited = false;
    private BufferCache bufferCache;
    private final InodeCache inodeCache;
    private Tools tools;
    private Clock clock;
    private final int deviceID;
    
    
    /**
     * Constructor for FatFileSystem in specified readOnly mode
     * @param readOnly
     */
    public FatFileSystem(boolean readOnly)  
    {
        drive = (BlockIO)InitialNaming.getInitialNaming().lookup("BIOFS_RW");
        try {
            fat = Fat.create(getApi());
        } catch (IOException ex) {
            //Logger.getLogger(FatFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        inodeCache = new InodeCache();
        deviceID = 1;
    }

    public FatFileSystem(BlockIO bio) {
        drive = bio;
        try {
            fat = Fat.create(bio);
        } catch (IOException ex) {
            //Logger.getLogger(FatFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        inodeCache = new InodeCache();
        deviceID = 1;
    }

    /**
     * Gets the file for the given entry.
     *
     * @param entry
     * @return 
     */
    public synchronized FatFile getFile(FatDirEntry entry) {
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
    protected RegularFile createFile(Node entry) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     *
     * @param entry
     * @return 
     * @throws java.io.IOException
     */
    protected Directory createDirectory(Node entry) throws IOException {
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
    public Node getRootNode() {
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
    public Node getNode(int identifier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDeviceID() {
        return deviceID; /* FIXME */
    }

    public CodePage getCodePage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

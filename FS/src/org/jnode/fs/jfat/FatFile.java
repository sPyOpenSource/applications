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
import jx.fs.FSAttribute;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NotExistException;
import jx.fs.Permission;
import jx.fs.PermissionException;
import jx.zero.Memory;

public class FatFile extends FatEntry
{
    public FatFile(FatFileSystem fs, FatDirectory parent, FatRecord record) {
        super(fs, parent, record);
    }
    
    public FatFile(FatFileSystem fs){
        super(fs);
    }

    @Override
    public boolean isFile() {
        return true;
    }

    /*public FSFile getFile() {
        return this;
    }*/

    @Override
    public int getLength() {
        return getEntry().getLength();
    }

    private void freeClusters(int oldLength, int newLength) throws IOException {
        if (newLength >= oldLength) throw new UnsupportedOperationException("new[" + newLength + "] >= old["
            + oldLength + "]");

        final int clusterSize = getFatFileSystem().getFat().getClusterSize();
        final int oldClusters = (oldLength + clusterSize - 1) / clusterSize;
        final int newClusters = (newLength + clusterSize - 1) / clusterSize;

        getChain().free(oldClusters - newClusters);
    }

    public void setLength(int length) throws IOException {
        int oldLength = getLength();
        if (length == oldLength)
            return;

        if (length < oldLength)
            freeClusters(oldLength, length);

        getEntry().setLength(length);
        getEntry().setStartCluster(getChain().getStartCluster());
        flush();
    }

    /**
     * Discards all file data and resets the length to zero.
     */
    void truncate() throws IOException {
        setLength(0);
    }

    @Override
    public int read(Memory dst, int offset, int length)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        if (length <= 0 || offset < 0)
            return 0;

        int available = getLength() - offset;
        if (available <= 0)
            return 0;
        int toRead = Math.min(length, available);
        try {
            getChain().read(offset, dst, toRead);
        } catch (IOException ex) {
            return 0;
        }
        return toRead;
    }

    @Override
    public int write(Memory src, int offset, int length)// throws InodeIOException, NoFileInodeException, NotExistException, PermissionException 
    {
        if (length <= 0 || offset < 0)
            return 0;
        try {
            getChain().write(offset, src, length);
            int newLength = offset + length;
            if (newLength > getLength())
                setLength(newLength);
            return length;
        } catch (IOException ex) {
            throw new Error("FatFile.write failed: " + ex);
        }
    }

    /*public void seek(long offset) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(0);
        write(offset, buf);
    }

    public void write(long offset, ByteBuffer src) throws IOException {
        if (offset < 0) throw new IllegalArgumentException("offset<0");

        long lst = offset + src.remaining();
        long length = 9;//getLength();

        FatChain chain = getChain();

        //chain.write(length, offset, src);

        //if (lst > length) getEntry().setLength(lst);

        //if (lst != offset) setLastModified(System.currentTimeMillis());

        //flush();
    }*/

    /*@Override
    public byte[] getSlackSpace() throws IOException {
        int clusterSize = ((FatFileSystem) getFileSystem()).getClusterSize();
        Fat fat = ((FatFileSystem) getFileSystem()).getFat();

        int offset = (int) (getLength() % clusterSize);
        int slackSpaceSize = clusterSize - offset;

        if (slackSpaceSize == clusterSize) {
            return new byte[0];
        }

        byte[] slackSpace = new byte[slackSpaceSize];

        if (getEntry().isFreeDirEntry()) {
            int cluster = getStartCluster() + (int) (getLength() / clusterSize);
            fat.readCluster(cluster, offset, ByteBuffer.wrap(slackSpace));
        } else {
            getChain().read(getLength(), ByteBuffer.wrap(slackSpace));
        }

        return slackSpace;
    }

    @Override
    public String toString() {
        return String.format("FatFile [%s] index:%d size:%d", getName(), getIndex(), getLength());
    }

    public String toDebugString() {
        StrWriter out = new StrWriter();

        out.println("*******************************************");
        out.println("FatFile");
        out.println("*******************************************");
        out.println("Index\t\t" + getIndex());
        out.println(toStringValue());
        out.println("Length\t\t" + getLength());
        out.print("*******************************************");

        return out.toString();
    }*/
    
    @Override
    public jx.fs.Node getNode(String name) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	if (i_released)	throw new NotExistException();
	throw new NoDirectoryInodeException();
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

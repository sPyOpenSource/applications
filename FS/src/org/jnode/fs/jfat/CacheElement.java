package org.jnode.fs.jfat;

import java.io.IOException;
import jx.devices.bio.BlockIO;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;

  public class CacheElement {
    /**
     * CacheKey element is allocated and its reference is stored here to
     * avoid to allocate new CacheKey objects at runtime
     * <p/>
     * In this way .. just one global key will be enough to access
     * CacheElements
     */
    private boolean dirty;
    private final CacheKey address;
    private Memory elem;
    private final int elementSize = 512;
    private BlockIO api;

    public CacheElement() {
        this.dirty = false;
        this.address = new CacheKey();
        try {
            MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
            // FAT-12 reads in two byte chunks so add an extra element to prevent an array index out of bounds exception
            // when reading in the last element
            elem = memMgr.alloc(elementSize);
        } catch (NullPointerException e) {
        }
    }

    public CacheElement(BlockIO api) {
        this.dirty = false;
            this.address = new CacheKey();
            try {
                MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
                // FAT-12 reads in two byte chunks so add an extra element to prevent an array index out of bounds exception
                // when reading in the last element
                elem = memMgr.alloc(elementSize);
            } catch (NullPointerException e) {
            }
        this.api = api;
    }

    public boolean isFree() {
        return address.isFree();
    }

    public CacheKey getAddress() {
        return address;
    }

    public Memory getData() {
        return elem;
    }

    /**
     * some more work is needed in read and write to handle the multiple fat
     * availability we have to correcly handle the exception to be sure that
     * if we have at least a correct fat we get it - gvt
     * @param address
     * @throws java.io.IOException
     */
    public void read(int address) throws IOException {
       /* if (!isFree())
            throw new IllegalArgumentException("cannot read a busy element");*/

        this.address.set(address);
        Debug.out.println(address);
        api.readSectors(address, 1, elem, true);
        Debug.out.println("ok");
    }

    private void write() throws IOException {
        if (isFree())
            throw new IllegalArgumentException("cannot write a free element");

        int addr = address.get() * elementSize;

        /*for (int i = 0; i < nrfats; i++) {
            api.writeSectors(address.get(), 1, elem, true);
            addr += fatsize;
        }*/
    }

    private boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        dirty = true;
    }

    public void flush() throws IOException {
        if (isDirty()) {
            write();
            dirty = false;
        }
    }

    public void free() throws IOException {
        if (isFree())
            throw new IllegalArgumentException("cannot free a free element");
        flush();
        address.free();
    }

    /*public String toString() {
        StrWriter out = new StrWriter();

        out.print("address=" + address.get() + " dirty=" + dirty);

        return out.toString();
    }*/
}
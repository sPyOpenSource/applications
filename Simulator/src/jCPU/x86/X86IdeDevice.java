package jCPU.x86;

import java.io.*;

/**
 * Base class for an IDE/ATAPI device (hard drive or CDROM).
 *
 * <p>Provides common sector-based storage backed by a file, plus the
 * identify-data block returned by the IDENTIFY DEVICE / IDENTIFY
 * PACKET DEVICE commands.</p>
 */
public abstract class X86IdeDevice
{
    /** Sector size in bytes. */
    protected static final int SECTOR_SIZE = 512;

    /** Number of sectors on this device. */
    protected final int sectorCount;

    /** Backing file, or null for a purely virtual device. */
    protected final RandomAccessFile image;

    /** Device name shown in identify data. */
    protected final String model;

    /** Device type: "ATA" for hard disk, "ATAPI" for CDROM. */
    protected final boolean atapi;

    protected X86IdeDevice(String model, int sectorCount, boolean atapi, String imagePath)
            throws IOException
    {
        this.model = model;
        this.sectorCount = sectorCount;
        this.atapi = atapi;
        this.image = (imagePath == null) ? null : new RandomAccessFile(imagePath, "rw");
    }

    /** Read one sector into the given buffer at buffer offset 0. */
    public abstract void readSector(int lba, byte[] buf);

    /** Write one sector from the given buffer at buffer offset 0. */
    public abstract void writeSector(int lba, byte[] buf);

    /** Whether this device is an ATAPI (packet) device. */
    public boolean isAtapi() { return atapi; }

    /** Number of sectors. */
    public int getSectorCount() { return sectorCount; }

    /** Model string (padded/truncated to 40 chars for identify data). */
    public String getModel() { return model; }

    /** Close the backing image. */
    public void close() throws IOException
    {
        if (image != null) image.close();
    }
}

package jCPU.x86;

import java.io.*;

/**
 * PC floppy disk drive backed by a raw sector image file.
 *
 * <p>The image is a flat file of {@code sectorCount * 512} bytes, read and
 * written one sector at a time.  A 1.44 MB floppy uses 2880 sectors.  This is
 * the device the BIOS reads the boot sector from when booting from floppy
 * (drive {@code 0x00}).</p>
 */
public class X86FloppyDrive extends X86IdeDevice
{
    private final byte[] sectorBuf = new byte[SECTOR_SIZE];

    public X86FloppyDrive(String model, int sectorCount, String imagePath)
            throws IOException
    {
        super(model, sectorCount, false, imagePath);

        // Pre-create / zero-fill the image file so the size is stable.
        if (image != null)
        {
            image.setLength(sectorCount * SECTOR_SIZE);
        }
    }

    @Override
    public void readSector(int lba, byte[] buf)
    {
        if (image == null)
        {
            java.util.Arrays.fill(buf, (byte) 0);
            return;
        }
        try
        {
            image.seek((long) lba * SECTOR_SIZE);
            image.readFully(sectorBuf);
            System.arraycopy(sectorBuf, 0, buf, 0, SECTOR_SIZE);
        }
        catch (IOException e)
        {
            java.util.Arrays.fill(buf, (byte) 0);
        }
    }

    @Override
    public void writeSector(int lba, byte[] buf)
    {
        if (image == null) return;
        try
        {
            image.seek((long) lba * SECTOR_SIZE);
            image.write(buf, 0, SECTOR_SIZE);
        }
        catch (IOException ignore) { }
    }
}

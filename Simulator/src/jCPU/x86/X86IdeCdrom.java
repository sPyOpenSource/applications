package jCPU.x86;

import java.io.*;

/**
 * IDE/ATAPI CDROM device.
 *
 * <p>An ATAPI (packet) device responds to IDENTIFY PACKET DEVICE
 * (0xA0) and accepts PACKET commands (also opcode 0xA0 with the
 * feature register set appropriately).  Sector reads are served from
 * the backing image file if one was provided (raw sector access, no
 * ISO9660/El Torito parsing yet).</p>
 */
public class X86IdeCdrom extends X86IdeDevice
{
    private final byte[] sectorBuf = new byte[SECTOR_SIZE];

    public X86IdeCdrom(String model, int sectorCount, String imagePath)
            throws IOException
    {
        super(model, sectorCount, true, imagePath);
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
        // CDROM is read-only.
    }
}

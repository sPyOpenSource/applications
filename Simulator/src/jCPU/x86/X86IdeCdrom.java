package jCPU.x86;

import java.io.*;

/**
 * IDE/ATAPI CDROM device.
 *
 * <p>An ATAPI (packet) device responds to IDENTIFY PACKET DEVICE
 * (0xA0) and accepts PACKET commands (also opcode 0xA0 with the
 * feature register set appropriately).  This implementation returns a
 * minimal but valid identify block advertising a CDROM, and treats any
 * sector reads as returning zero-filled sectors (no real ISO9660 parsing
 * is implemented).</p>
 */
public class X86IdeCdrom extends X86IdeDevice
{
    public X86IdeCdrom(String model, int sectorCount, String imagePath)
            throws IOException
    {
        super(model, sectorCount, true, imagePath);
    }

    @Override
    public void readSector(int lba, byte[] buf)
    {
        // No ISO9660 filesystem support yet: return blank sectors.
        java.util.Arrays.fill(buf, (byte) 0);
    }

    @Override
    public void writeSector(int lba, byte[] buf)
    {
        // CDROM is read-only.
    }
}

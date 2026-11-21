package jCPU.x86;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Reads a GPT laid out as the UEFI specification (and sgdisk/fdisk) write it.
 */
public class X86GptTest
{
    /** EFI System Partition type GUID as stored on disk (mixed endian). */
    private static final byte[] ESP_GUID_BYTES = {
        0x28, 0x73, 0x2A, (byte) 0xC1, 0x1F, (byte) 0xF8, (byte) 0xD2, 0x11,
        (byte) 0xBA, 0x4B, 0x00, (byte) 0xA0, (byte) 0xC9, 0x3E, (byte) 0xC9, 0x3B
    };

    private static void put32(byte[] b, int off, long v)
    {
        for (int i = 0; i < 4; i++) {
            b[off + i] = (byte) (v >>> (8 * i));
        }
    }

    private static void put64(byte[] b, int off, long v)
    {
        for (int i = 0; i < 8; i++) {
            b[off + i] = (byte) (v >>> (8 * i));
        }
    }

    /** 34-sector disk: header at LBA 1, 128 entries of 128 bytes from LBA 2. */
    private static byte[][] disk(long espFirst, long espLast)
    {
        byte[][] sectors = new byte[34][512];
        byte[] hdr = sectors[1];
        System.arraycopy("EFI PART".getBytes(), 0, hdr, 0, 8);
        put32(hdr, 8, 0x00010000);  // revision
        put32(hdr, 12, 92);         // header size
        put64(hdr, 24, 1);          // MyLBA
        put64(hdr, 72, 2);          // PartitionEntryLBA
        put32(hdr, 80, 128);        // NumberOfPartitionEntries
        put32(hdr, 84, 128);        // SizeOfPartitionEntry
        byte[] e = sectors[2];
        System.arraycopy(ESP_GUID_BYTES, 0, e, 0, 16);
        e[16] = 1;                  // unique GUID, anything non-zero
        put64(e, 32, espFirst);
        put64(e, 40, espLast);
        e[56] = 'E';
        e[58] = 'S';
        e[60] = 'P';
        return sectors;
    }

    @Test
    public void testFindsEfiSystemPartition()
    {
        byte[][] sectors = disk(2048, 81886);
        X86Gpt gpt = new X86Gpt(lba -> lba < sectors.length ? sectors[lba].clone() : new byte[512]);

        assertTrue(gpt.parse(1));
        assertEquals(1, gpt.getPartitions().size());
        X86Gpt.Partition esp = gpt.findEfiSystemPartition();
        assertNotNull(esp);
        assertEquals(2048, esp.firstLba);
        assertEquals(81886, esp.lastLba);
        assertEquals("ESP", esp.name);
    }

    @Test
    public void testRejectsDiskWithoutSignature()
    {
        byte[][] sectors = disk(2048, 81886);
        sectors[1][0] = 'X';
        X86Gpt gpt = new X86Gpt(lba -> lba < sectors.length ? sectors[lba].clone() : new byte[512]);

        assertFalse(gpt.parse(1));
    }
}

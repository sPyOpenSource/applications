package jCPU.x86;

import java.util.ArrayList;
import java.util.List;

/**
 * GUID Partition Table (GPT) reader.
 *
 * <p>Reads the GPT header and partition entry array from a disk image accessed
 * through a {@link SectorReader}.  Provides a list of partitions so that a UEFI
 * firmware can locate the EFI System Partition (type C12A7328-F81F-11D2-BA4B-00A0C93EC93B).</p>
 */
public class X86Gpt
{
    /** EFI System Partition GUID. */
    public static final String EFI_SYSTEM_PARTITION_GUID =
            "C12A7328-F81F-11D2-BA4B-00A0C93EC93B";

    private final X86FatFileSystem.SectorReader reader;

    private int headerLba;
    private int entryStartLba;
    private int entryCount;
    private int entrySize;

    private final List<Partition> partitions = new ArrayList<>();

    public X86Gpt(X86FatFileSystem.SectorReader reader)
    {
        this.reader = reader;
    }

    public boolean parse(int lba)
    {
        partitions.clear();

        // GPT header lives at LBA 1 by default; check both LBA 1 and the passed LBA
        byte[] hdr = reader.readSector(lba);
        if (hdr == null || hdr.length < 92)
        {
            hdr = reader.readSector(1);
            if (hdr == null || hdr.length < 92)
            {
                return false;
            }
            lba = 1;
        }

        // Signature "EFI PART" at offset 0
        if (hdr[0] != 'E' || hdr[1] != 'F' || hdr[2] != 'I' ||
            hdr[3] != ' ' || hdr[4] != 'P' || hdr[5] != 'A' ||
            hdr[6] != 'R' || hdr[7] != 'T')
        {
            return false;
        }

        headerLba = lba;
        // Offset 72: PartitionEntryLBA (8 bytes), 80: NumberOfPartitionEntries,
        // 84: SizeOfPartitionEntry
        entryStartLba = (int) readLittleEndian64(hdr, 72);
        entryCount = readLittleEndian32(hdr, 80);
        entrySize = readLittleEndian32(hdr, 84);

        if (entryStartLba == 0 || entryCount == 0 || entrySize == 0)
        {
            return false;
        }

        // Read partition entries (each entrySize bytes, starting at entryStartLba)
        int entriesPerSector = 512 / entrySize;
        int sectorsNeeded = (entryCount * entrySize + 511) / 512;
        for (int s = 0; s < sectorsNeeded; s++)
        {
            byte[] sector = reader.readSector(entryStartLba + s);
            if (sector == null)
            {
                break;
            }
            for (int off = 0; off + entrySize <= sector.length; off += entrySize)
            {
                int number = s * entriesPerSector + off / entrySize + 1;
                Partition p = parseEntry(sector, off, number);
                if (p != null && p.firstLba != 0)
                {
                    partitions.add(p);
                }
            }
        }
        return true;
    }

    public List<Partition> getPartitions()
    {
        return partitions;
    }

    public Partition findEfiSystemPartition()
    {
        for (Partition p : partitions)
        {
            if (EFI_SYSTEM_PARTITION_GUID.equalsIgnoreCase(p.typeGuid))
            {
                return p;
            }
        }
        return null;
    }

    private Partition parseEntry(byte[] buf, int off, int number)
    {
        // Partition type GUID (16 bytes)
        String typeGuid = guidToString(buf, off);
        // Unique partition GUID (16 bytes)
        String uniqueGuid = guidToString(buf, off + 16);
        // First LBA (8 bytes)
        long firstLba = readLittleEndian64(buf, off + 32);
        // Last LBA (8 bytes)
        long lastLba = readLittleEndian64(buf, off + 40);
        // Attributes (8 bytes)
        long attributes = readLittleEndian64(buf, off + 48);
        // Partition name (72 bytes, UTF-16LE)
        String name = readUtf16Name(buf, off + 56);

        // Unused entry: all zeros
        boolean empty = true;
        for (int i = 0; i < buf.length - off && i < entrySize; i++)
        {
            if (buf[off + i] != 0)
            {
                empty = false;
                break;
            }
        }
        if (empty)
        {
            return null;
        }

        return new Partition(typeGuid, uniqueGuid, (int) firstLba,
                             (int) lastLba, attributes, name, number);
    }

    private String guidToString(byte[] buf, int off)
    {
        // GUID is stored as: time_low(4 LE), time_mid(2 LE), time_hi(2 LE),
        // clock_seq(2 bytes), node(6 bytes)
        int timeLow = readLittleEndian32(buf, off);
        int timeMid = readLittleEndian16(buf, off + 4);
        int timeHi = readLittleEndian16(buf, off + 6);
        return String.format("%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                timeLow, timeMid, timeHi,
                buf[off + 8] & 0xFF, buf[off + 9] & 0xFF,
                buf[off + 10] & 0xFF, buf[off + 11] & 0xFF,
                buf[off + 12] & 0xFF, buf[off + 13] & 0xFF,
                buf[off + 14] & 0xFF, buf[off + 15] & 0xFF);
    }

    private int readLittleEndian16(byte[] buf, int off)
    {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);
    }

    private int readLittleEndian32(byte[] buf, int off)
    {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8) |
               ((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
    }

    private long readLittleEndian64(byte[] buf, int off)
    {
        return (readLittleEndian32(buf, off) & 0xFFFFFFFFL) |
               ((long) (readLittleEndian32(buf, off + 4) & 0xFFFFFFFFL) << 32);
    }

    private String readUtf16Name(byte[] buf, int off)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 72; i += 2)
        {
            int ch = (buf[off + i] & 0xFF) | ((buf[off + i + 1] & 0xFF) << 8);
            if (ch == 0)
            {
                break;
            }
            sb.append((char) ch);
        }
        return sb.toString();
    }

    /** A single GPT partition entry. */
    public static class Partition
    {
        public final String typeGuid;
        public final String uniqueGuid;
        public final int firstLba;
        public final int lastLba;
        public final long attributes;
        public final String name;
        /** 1-based index in the partition entry array, 0 if unknown. */
        public final int number;

        public Partition(String typeGuid, String uniqueGuid, int firstLba,
                         int lastLba, long attributes, String name)
        {
            this(typeGuid, uniqueGuid, firstLba, lastLba, attributes, name, 0);
        }

        public Partition(String typeGuid, String uniqueGuid, int firstLba,
                         int lastLba, long attributes, String name, int number)
        {
            this.number = number;
            this.typeGuid = typeGuid;
            this.uniqueGuid = uniqueGuid;
            this.firstLba = firstLba;
            this.lastLba = lastLba;
            this.attributes = attributes;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return String.format("GPT[%s] %s LBA %d-%d %s",
                    typeGuid, name, firstLba, lastLba,
                    (attributes & 1) != 0 ? "(bootable)" : "");
        }
    }
}

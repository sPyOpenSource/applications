package jCPU.x86;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only FAT12 / FAT16 / FAT32 filesystem reader.
 *
 * <p>Sectors are supplied by a {@link SectorReader} callback, which allows
 * the filesystem to live on the IDE disk image without depending on a host
 * {@link java.io.File}.  Only enough of FAT is implemented to locate files
 * in the root directory and read their contents: enough for loading a UEFI
 * boot loader from an EFI System Partition.</p>
 */
public class X86FatFileSystem
{
    public interface SectorReader
    {
        byte[] readSector(int lba);
    }

    private final SectorReader reader;

    /* BIOS Parameter Block fields */
    private int bytesPerSector;
    private int sectorsPerCluster;
    private int reservedSectors;
    private int fatCount;
    private int rootEntryCount;
    private int totalSectors16;
    private int mediaDescriptor;
    private int fatSize16;
    private int totalSectors32;
    private int fatSize32;

    private int fatType;          // 12, 16, or 32
    private int fatOffset;        // LBA of first FAT
    private int rootDirOffset;    // LBA of root directory (FAT12/16) or cluster (FAT32)
    private int rootDirCluster;   // Root directory cluster (FAT32 only)
    private int dataRegionOffset; // LBA of data region

    private boolean mounted;
    public X86FatFileSystem(SectorReader reader)
    {
        this.reader = reader;
    }

    public boolean mount(int lba)
    {
        byte[] bs = reader.readSector(lba);
        if (bs == null || bs.length < 90)
        {
            return false;
        }

        // BPB starts at offset 11
        bytesPerSector = (bs[11] & 0xFF) | ((bs[12] & 0xFF) << 8);
        sectorsPerCluster = bs[13] & 0xFF;
        reservedSectors = (bs[14] & 0xFF) | ((bs[15] & 0xFF) << 8);
        fatCount = bs[16] & 0xFF;
        rootEntryCount = (bs[17] & 0xFF) | ((bs[18] & 0xFF) << 8);
        totalSectors16 = (bs[19] & 0xFF) | ((bs[20] & 0xFF) << 8);
        mediaDescriptor = bs[21] & 0xFF;
        fatSize16 = (bs[22] & 0xFF) | ((bs[23] & 0xFF) << 8);
        totalSectors32 = (bs[32] & 0xFF) | ((bs[33] & 0xFF) << 8) |
                         ((bs[34] & 0xFF) << 16) | ((bs[35] & 0xFF) << 24);
        fatSize32 = (bs[36] & 0xFF) | ((bs[37] & 0xFF) << 8) |
                    ((bs[38] & 0xFF) << 16) | ((bs[39] & 0xFF) << 24);

        // FAT32 root directory cluster (offset 44 in boot sector)
        rootDirCluster = (bs[44] & 0xFF) | ((bs[45] & 0xFF) << 8) |
                         ((bs[46] & 0xFF) << 16) | ((bs[47] & 0xFF) << 24);

        if (bytesPerSector == 0 || sectorsPerCluster == 0)
        {
            return false;
        }

        fatOffset = lba + reservedSectors;

        int rootDirSectors = ((rootEntryCount * 32) + (bytesPerSector - 1)) / bytesPerSector;
        rootDirOffset = fatOffset + (fatCount * (fatSize32 != 0 ? fatSize32 : fatSize16));
        int dataRegionStart = rootDirOffset + rootDirSectors;
        int totalSectors = totalSectors32 != 0 ? totalSectors32 : totalSectors16;
        int dataSectors = totalSectors - dataRegionStart;

        // Determine FAT type by counting data clusters
        int clusters = dataSectors / sectorsPerCluster;
        if (clusters < 4085)
        {
            fatType = 12;
        }
        else if (clusters < 65525)
        {
            fatType = 16;
        }
        else
        {
            fatType = 32;
        }

        dataRegionOffset = dataRegionStart;
        mounted = true;
        return true;
    }

    public boolean isMounted()
    {
        return mounted;
    }

    public int getFatType()
    {
        return fatType;
    }

    /**
     * Look up a file in the root directory by its 8.3 name (e.g. "BOOTX64 EFI").
     * Returns the starting cluster (FAT32) or sector offset (FAT12/16), and
     * the file size in bytes.
     */
    /**
     * Look up a file in the root directory by its 8.3 name (e.g. "BOOTX64 EFI").
     * Supports path separators {@code \} and {@code /} for subdirectories,
     * e.g. {@code "EFI\BOOT\BOOTX64.EFI"}.
     * Returns the starting cluster (FAT32) or sector offset (FAT12/16), and
     * the file size in bytes.
     */
    public FileEntry findFile(String name)
    {
        if (!mounted)
        {
            return null;
        }

        // Normalize path separators
        String normalized = name.replace('/', '\\');
        String[] parts = normalized.split("\\\\");

        int cluster = (fatType == 32) ? rootDirCluster : -1;

        // Traverse each directory component
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.isEmpty())
            {
                continue;
            }

            // Normalize component to 11-byte 8.3
            byte[] target = new byte[11];
            for (int j = 0; j < 11; j++)
            {
                target[j] = ' ';
            }
            String[] nameParts = part.split("\\.");
            String base = nameParts[0].toUpperCase();
            String ext = nameParts.length > 1 ? nameParts[1].toUpperCase() : "";
            for (int j = 0; j < Math.min(base.length(), 8); j++)
            {
                target[j] = (byte) base.charAt(j);
            }
            for (int j = 0; j < Math.min(ext.length(), 3); j++)
            {
                target[8 + j] = (byte) ext.charAt(j);
            }

            // Read directory sectors starting from cluster (or root for FAT12/16 first component)
            List<byte[]> dirSectors;
            if (cluster < 0 && i == 0)
            {
                dirSectors = readRootDirectorySectors();
            }
            else
            {
                dirSectors = readDirectorySectors(cluster);
            }

            FileEntry found = searchDirectory(dirSectors, target);
            if (found == null)
            {
                return null;
            }

            // If this is the last component, return it
            if (i == parts.length - 1)
            {
                return found;
            }

            // Otherwise, descend into this directory
            cluster = found.startCluster;
        }
        return null;
    }

    /**
     * Search a list of directory sectors for an 11-byte 8.3 target name.
     * Returns the matching FileEntry, or null if not found.
     */
    private FileEntry searchDirectory(List<byte[]> dirSectors, byte[] target)
    {
        for (byte[] sector : dirSectors)
        {
            if (sector == null)
            {
                continue;
            }
            for (int off = 0; off + 32 <= sector.length; off += 32)
            {
                if (sector[off] == 0x00)
                {
                    return null; // end of directory
                }
                if (sector[off] == 0xE5)
                {
                    continue; // deleted entry
                }
                if ((sector[off + 11] & 0x08) != 0)
                {
                    continue; // volume label
                }
                boolean match = true;
                for (int i = 0; i < 11; i++)
                {
                    if (sector[off + i] != target[i])
                    {
                        match = false;
                        break;
                    }
                }
                if (match)
                {
                    int size = (sector[off + 28] & 0xFF) |
                               ((sector[off + 29] & 0xFF) << 8) |
                               ((sector[off + 30] & 0xFF) << 16) |
                               ((sector[off + 31] & 0xFF) << 24);
                    int startCluster;
                    if (fatType == 32)
                    {
                        startCluster = (sector[off + 26] & 0xFF) |
                                       ((sector[off + 27] & 0xFF) << 8) |
                                       ((sector[off + 20] & 0xFF) << 16) |
                                       ((sector[off + 21] & 0xFF) << 24);
                    }
                    else
                    {
                        startCluster = (sector[off + 26] & 0xFF) |
                                       ((sector[off + 27] & 0xFF) << 8);
                    }
                    return new FileEntry(startCluster, size);
                }
            }
        }
        return null;
    }

    /**
     * Read all sectors of a directory cluster chain starting at the given cluster.
     */
    private List<byte[]> readDirectorySectors(int startCluster)
    {
        List<byte[]> sectors = new ArrayList<>();
        int cluster = startCluster;
        while (cluster >= 2 && cluster < 0x0FFFFFF8)
        {
            int sectorLba = clusterToSector(cluster);
            for (int i = 0; i < sectorsPerCluster; i++)
            {
                sectors.add(reader.readSector(sectorLba + i));
            }
            cluster = nextCluster32(cluster);
        }
        return sectors;
    }

    /**
     * Read all sectors of the root directory.  For FAT12/16 this is a linear
     * range of sectors starting at {@link #rootDirOffset}.  For FAT32 this
     * follows the cluster chain starting at {@link #rootDirCluster}.
     */
    private List<byte[]> readRootDirectorySectors()
    {
        List<byte[]> sectors = new ArrayList<>();
        if (fatType != 32)
        {
            int rootDirSectors = ((rootEntryCount * 32) + (bytesPerSector - 1)) / bytesPerSector;
            for (int s = 0; s < rootDirSectors; s++)
            {
                sectors.add(reader.readSector(rootDirOffset + s));
            }
        }
        else
        {
            // FAT32: follow the cluster chain starting at rootDirCluster
            int cluster = rootDirCluster;
            while (cluster >= 2 && cluster < 0x0FFFFFF8)
            {
                int sectorLba = clusterToSector(cluster);
                for (int i = 0; i < sectorsPerCluster; i++)
                {
                    sectors.add(reader.readSector(sectorLba + i));
                }
                cluster = nextCluster32(cluster);
            }
        }
        return sectors;
    }

    /**
     * Read the next cluster in a FAT32 chain.
     */
    private int nextCluster32(int cluster)
    {
        int fatByteOffset = cluster * 4; // byte offset within FAT
        int fatSectorLba = fatOffset + fatByteOffset / bytesPerSector;
        int offset = fatByteOffset % bytesPerSector;
        byte[] fat = reader.readSector(fatSectorLba);
        if (fat == null)
        {
            return 0x0FFFFFF8;
        }
        if (offset + 4 <= bytesPerSector)
        {
            return (fat[offset] & 0xFF) |
                   ((fat[offset + 1] & 0xFF) << 8) |
                   ((fat[offset + 2] & 0xFF) << 16) |
                   ((fat[offset + 3] & 0xFF) << 24);
        }
        // FAT entry straddles sector boundary
        byte[] fat2 = reader.readSector(fatSectorLba + 1);
        if (fat2 == null)
        {
            return 0x0FFFFFF8;
        }
        int b0 = fat[offset] & 0xFF;
        int b1 = fat[offset + 1] & 0xFF;
        int b2 = fat2[0] & 0xFF;
        int b3 = fat2[1] & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    /**
     * List all entries in the root directory.  Returns a list of
     * {@code String} descriptions like "BOOTX64 EFI 12345".
     */
    public List<String> listRoot()
    {
        List<String> entries = new ArrayList<>();
        if (!mounted)
        {
            return entries;
        }

        List<byte[]> dirSectors = readRootDirectorySectors();
        for (byte[] sector : dirSectors)
        {
            if (sector == null)
            {
                continue;
            }
            for (int off = 0; off + 32 <= sector.length; off += 32)
            {
                if (sector[off] == 0x00)
                {
                    break; // end of directory
                }
                if (sector[off] == 0xE5)
                {
                    continue; // deleted entry
                }
                if ((sector[off + 11] & 0x08) != 0)
                {
                    continue; // volume label
                }
                StringBuilder name = new StringBuilder();
                for (int i = 0; i < 8; i++)
                {
                    char c = (char) sector[off + i];
                    if (c == ' ') break;
                    name.append(c);
                }
                name.append('.');
                for (int i = 8; i < 11; i++)
                {
                    char c = (char) sector[off + i];
                    if (c == ' ') break;
                    name.append(c);
                }
                int size = (sector[off + 28] & 0xFF) |
                           ((sector[off + 29] & 0xFF) << 8) |
                           ((sector[off + 30] & 0xFF) << 16) |
                           ((sector[off + 31] & 0xFF) << 24);
                entries.add(name.toString() + " " + size);
            }
        }
        return entries;
    }

    /**
     * Read the entire file content into a newly allocated byte array.
     */
    public byte[] readFile(FileEntry entry)
    {
        if (!mounted || entry == null)
        {
            return null;
        }

        List<Byte> data = new ArrayList<>();
        int cluster = entry.startCluster;
        boolean fat32 = (fatType == 32);
        int eoc = fat32 ? 0x0FFFFFF8 : 0xFFF8;
        while (cluster >= 2 && cluster < eoc)
        {
            int sectorLba = clusterToSector(cluster);
            for (int i = 0; i < sectorsPerCluster; i++)
            {
                byte[] sector = reader.readSector(sectorLba + i);
                if (sector != null)
                {
                    for (byte b : sector)
                    {
                        data.add(b);
                    }
                }
            }
            cluster = fat32 ? nextCluster32(cluster) : nextCluster(cluster);
            if (data.size() >= entry.size)
            {
                break;
            }
        }

        byte[] buf = new byte[Math.min(data.size(), entry.size)];
        for (int i = 0; i < buf.length; i++)
        {
            buf[i] = data.get(i);
        }
        return buf;
    }

    /* ---------------- internal helpers ---------------- */

    private int clusterToSector(int cluster)
    {
        return dataRegionOffset + (cluster - 2) * sectorsPerCluster;
    }

    private int nextCluster(int cluster)
    {
        if (fatType == 12)
        {
            int fatByteOffset = cluster * 3 / 2; // byte offset within FAT
            int fatSectorLba = fatOffset + fatByteOffset / bytesPerSector;
            int offset = fatByteOffset % bytesPerSector;
            byte[] fat = reader.readSector(fatSectorLba);
            if (fat == null)
            {
                return 0xFFF8;
            }
            if (offset + 2 > bytesPerSector)
            {
                // FAT12 entry straddles sector boundary
                byte[] fat2 = reader.readSector(fatSectorLba + 1);
                if (fat2 == null) return 0xFFF8;
                int b0 = fat[offset] & 0xFF;
                int b1 = fat2[0] & 0xFF;
                int value;
                if ((cluster & 1) == 0)
                {
                    value = b0 | (b1 << 4);
                }
                else
                {
                    value = (b0 >> 4) | (b1 << 8);
                }
                return value & 0xFFF;
            }
            int b0 = fat[offset] & 0xFF;
            int b1 = fat[offset + 1] & 0xFF;
            int value;
            if ((cluster & 1) == 0)
            {
                value = b0 | (b1 << 4);
            }
            else
            {
                value = (b0 >> 4) | (b1 << 8);
            }
            return value & 0xFFF;
        }
        else if (fatType == 16)
        {
            int fatByteOffset = cluster * 2; // byte offset within FAT
            int fatSectorLba = fatOffset + fatByteOffset / bytesPerSector;
            int offset = fatByteOffset % bytesPerSector;
            byte[] fat = reader.readSector(fatSectorLba);
            if (fat == null)
            {
                return 0xFFF8;
            }
            if (offset + 2 > bytesPerSector)
            {
                byte[] fat2 = reader.readSector(fatSectorLba + 1);
                if (fat2 == null) return 0xFFF8;
                return (fat[offset] & 0xFF) | ((fat2[0] & 0xFF) << 8);
            }
            return (fat[offset] & 0xFF) | ((fat[offset + 1] & 0xFF) << 8);
        }
        else
        {
            // FAT32
            int fatByte = fatOffset + cluster * 4;
            int sector = fatByte / bytesPerSector;
            int offset = fatByte % bytesPerSector;
            byte[] fat = reader.readSector(sector);
            if (fat == null)
            {
                return 0x0FFFFFF8;
            }
            if (offset + 4 <= bytesPerSector)
            {
                return (fat[offset] & 0xFF) |
                       ((fat[offset + 1] & 0xFF) << 8) |
                       ((fat[offset + 2] & 0xFF) << 16) |
                       ((fat[offset + 3] & 0xFF) << 24);
            }
            // FAT entry straddles sector boundary
            byte[] fat2 = reader.readSector(sector + 1);
            if (fat2 == null)
            {
                return 0x0FFFFFF8;
            }
            int b0 = fat[offset] & 0xFF;
            int b1 = fat[offset + 1] & 0xFF;
            int b2 = fat2[0] & 0xFF;
            int b3 = fat2[1] & 0xFF;
            return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }
    }

    /** Result of a directory lookup. */
    public static class FileEntry
    {
        public final int startCluster;
        public final int size;

        public FileEntry(int startCluster, int size)
        {
            this.startCluster = startCluster;
            this.size = size;
        }
    }
}

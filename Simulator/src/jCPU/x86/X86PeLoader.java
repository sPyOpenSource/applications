package jCPU.x86;

/**
 * Minimal PE32 / PE32+ (Portable Executable) loader.
 *
 * <p>Reads a PE image from a {@code byte[]} and provides the information
 * needed to copy sections into simulated memory and transfer control to the
 * entry point.  This is sufficient for loading UEFI boot loaders and EFI
 * applications (which are PE32+ for x64, PE32 for IA32).</p>
 */
public class X86PeLoader
{
    public final boolean pe32Plus;
    public final int entryPointRva;
    public final long imageBase;
    public final int sizeOfImage;
    public final int sizeOfHeaders;
    public final int sectionAlignment;
    public final int fileAlignment;
    /** Base relocation directory (data directory 5); size 0 when absent. */
    public final int relocRva;
    public final int relocSize;
    /** IMAGE_FILE_RELOCS_STRIPPED: the image can only run at its ImageBase. */
    public final boolean relocsStripped;

    private PeSection[] sections = new PeSection[0];

    public X86PeLoader(byte[] peImage)
    {
        boolean plus = false;
        int entry = 0, imageSize = 0, headersSize = 0, sectAlign = 1, fileAlign = 1;
        int relRva = 0, relSize = 0;
        boolean stripped = false;
        long base = 0;

        int optHeaderOff = -1, optHeaderSize = 0, numSections = 0;
        if (peImage != null && peImage.length >= 64)
        {
            // DOS header: e_lfanew at offset 0x3C, then "PE\0\0" and the COFF header
            int e_lfanew = readLittleEndian32(peImage, 0x3C);
            if (e_lfanew > 0 && e_lfanew + 24 <= peImage.length
                && peImage[e_lfanew] == 'P' && peImage[e_lfanew + 1] == 'E'
                && peImage[e_lfanew + 2] == 0 && peImage[e_lfanew + 3] == 0)
            {
                int coffOff = e_lfanew + 4;
                numSections = readLittleEndian16(peImage, coffOff + 2);
                stripped = (readLittleEndian16(peImage, coffOff + 18) & 0x0001) != 0;
                optHeaderSize = readLittleEndian16(peImage, coffOff + 16);
                if (coffOff + 20 + optHeaderSize <= peImage.length && optHeaderSize >= 64)
                {
                    optHeaderOff = coffOff + 20;
                }
            }
        }

        if (optHeaderOff >= 0)
        {
            // Optional header; PE32+ differs from PE32 only in the 8-byte ImageBase
            // and stack/heap sizes, which shift the data directories.
            plus = readLittleEndian16(peImage, optHeaderOff) == 0x20B;
            entry = readLittleEndian32(peImage, optHeaderOff + 16);
            base = plus ? readInt64(peImage, optHeaderOff + 24)
                        : readLittleEndian32(peImage, optHeaderOff + 28) & 0xFFFFFFFFL;
            sectAlign = readLittleEndian32(peImage, optHeaderOff + 32);
            fileAlign = readLittleEndian32(peImage, optHeaderOff + 36);
            imageSize = readLittleEndian32(peImage, optHeaderOff + 56);
            headersSize = readLittleEndian32(peImage, optHeaderOff + 60);
            int numDirs = readLittleEndian32(peImage, optHeaderOff + (plus ? 108 : 92));
            int dirs = optHeaderOff + (plus ? 112 : 96);
            if (numDirs > 5 && dirs + 6 * 8 <= optHeaderOff + optHeaderSize)
            {
                relRva = readLittleEndian32(peImage, dirs + 5 * 8);
                relSize = readLittleEndian32(peImage, dirs + 5 * 8 + 4);
            }

            // Section headers start right after the optional header
            int sectionTableOff = optHeaderOff + optHeaderSize;
            sections = new PeSection[numSections];
            for (int i = 0; i < numSections; i++)
            {
                int sOff = sectionTableOff + i * 40;
                if (sOff + 40 > peImage.length)
                {
                    sections = java.util.Arrays.copyOf(sections, i);
                    break;
                }
                String name = readSectionName(peImage, sOff);
                int vsize = readLittleEndian32(peImage, sOff + 8);
                int vaddr = readLittleEndian32(peImage, sOff + 12);
                int fsize = readLittleEndian32(peImage, sOff + 16);
                int faddr = readLittleEndian32(peImage, sOff + 20);
                sections[i] = new PeSection(name, vaddr, vsize, faddr, fsize);
            }
        }

        pe32Plus = plus;
        entryPointRva = entry;
        imageBase = base;
        sizeOfImage = imageSize;
        sizeOfHeaders = headersSize;
        sectionAlignment = sectAlign;
        fileAlignment = fileAlign;
        relocRva = relRva;
        relocSize = relSize;
        relocsStripped = stripped;
    }

    public PeSection[] getSections()
    {
        return sections;
    }

    /**
     * Lay the image out as it appears in memory (headers, then each section at
     * its RVA, uninitialised parts zero) and apply the base relocations for
     * loading at {@code loadAddress} instead of {@link #imageBase}.
     *
     * @return {@code sizeOfImage} bytes to copy to {@code loadAddress}, or
     *         null when the image has to move but its relocations were stripped
     */
    public byte[] layout(byte[] peImage, long loadAddress)
    {
        byte[] mem = new byte[sizeOfImage];
        System.arraycopy(peImage, 0, mem, 0, Math.min(sizeOfHeaders, Math.min(peImage.length, mem.length)));
        for (PeSection sec : sections)
        {
            int len = Math.min(sec.rawSize, peImage.length - sec.rawAddress);
            if (sec.virtualSize != 0)
            {
                len = Math.min(len, sec.virtualSize);
            }
            len = Math.min(len, mem.length - sec.virtualAddress);
            if (len > 0)
            {
                System.arraycopy(peImage, sec.rawAddress, mem, sec.virtualAddress, len);
            }
        }

        long delta = loadAddress - imageBase;
        if (delta == 0)
        {
            return mem;
        }
        if (relocsStripped)
        {
            return null;
        }
        // Blocks of: page RVA (4), block size (4), then 2-byte entries
        // (type in the top 4 bits, offset within the page in the low 12).
        int off = relocRva;
        int end = Math.min(relocRva + relocSize, mem.length);
        while (off + 8 <= end)
        {
            int page = readLittleEndian32(mem, off);
            int blockSize = readLittleEndian32(mem, off + 4);
            if (blockSize < 8)
            {
                break;
            }
            for (int e = off + 8; e + 2 <= off + blockSize && e + 2 <= end; e += 2)
            {
                int entry = readLittleEndian16(mem, e);
                int type = entry >>> 12;
                int at = page + (entry & 0xFFF);
                if (type == 3)          // IMAGE_REL_BASED_HIGHLOW
                {
                    writeLittleEndian(mem, at, readLittleEndian32(mem, at) + delta, 4);
                }
                else if (type == 10)    // IMAGE_REL_BASED_DIR64
                {
                    writeLittleEndian(mem, at, readInt64(mem, at) + delta, 8);
                }
                // type 0 (ABSOLUTE) is padding
            }
            off += blockSize;
        }
        return mem;
    }

    private static void writeLittleEndian(byte[] buf, int off, long value, int size)
    {
        for (int i = 0; i < size; i++)
        {
            buf[off + i] = (byte) (value >>> (8 * i));
        }
    }

    public int getEntryPoint()
    {
        return (int) (imageBase + entryPointRva);
    }

    public long getEntryPoint64()
    {
        return imageBase + entryPointRva;
    }

    /* ---------------- helpers ---------------- */

    private static int readLittleEndian16(byte[] buf, int off)
    {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8);
    }

    private static int readLittleEndian32(byte[] buf, int off)
    {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8) |
               ((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
    }

    /**
     * Reads an 8-byte little-endian field as a long.
     */
    private static long readInt64(byte[] buf, int off)
    {
        return readLittleEndian32(buf, off) & 0xFFFFFFFFL |
               ((long) readLittleEndian32(buf, off + 4) << 32);
    }

    private static String readSectionName(byte[] buf, int off)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++)
        {
            int b = buf[off + i] & 0xFF;
            if (b == 0)
            {
                break;
            }
            sb.append((char) b);
        }
        return sb.toString();
    }

    /** A single PE section. */
    public static class PeSection
    {
        public final String name;
        public final int virtualAddress;
        public final int virtualSize;
        public final int rawAddress;
        public final int rawSize;

        public PeSection(String name, int virtualAddress, int virtualSize,
                         int rawAddress, int rawSize)
        {
            this.name = name;
            this.virtualAddress = virtualAddress;
            this.virtualSize = virtualSize;
            this.rawAddress = rawAddress;
            this.rawSize = rawSize;
        }
    }
}

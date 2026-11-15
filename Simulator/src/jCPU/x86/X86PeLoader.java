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
    public final int imageBase;
    public final int sizeOfImage;
    public final int sectionAlignment;
    public final int fileAlignment;

    private PeSection[] sections;

    public X86PeLoader(byte[] peImage)
    {
        sections = new PeSection[0];

        if (peImage == null || peImage.length < 64)
        {
            pe32Plus = false;
            entryPointRva = 0;
            imageBase = 0;
            sizeOfImage = 0;
            sectionAlignment = 1;
            fileAlignment = 1;
            return;
        }

        // DOS header: e_lfanew at offset 0x3C
        int e_lfanew = readLittleEndian32(peImage, 0x3C);
        if (e_lfanew + 24 > peImage.length)
        {
            pe32Plus = false;
            entryPointRva = 0;
            imageBase = 0;
            sizeOfImage = 0;
            sectionAlignment = 1;
            fileAlignment = 1;
            return;
        }

        // PE signature "PE\0\0"
        if (peImage[e_lfanew] != 'P' || peImage[e_lfanew + 1] != 'E' ||
            peImage[e_lfanew + 2] != 0 || peImage[e_lfanew + 3] != 0)
        {
            pe32Plus = false;
            entryPointRva = 0;
            imageBase = 0;
            sizeOfImage = 0;
            sectionAlignment = 1;
            fileAlignment = 1;
            return;
        }

        int coffOff = e_lfanew + 4;

        // COFF header (20 bytes)
        int numSections = readLittleEndian16(peImage, coffOff + 2);
        int optHeaderSize = readLittleEndian16(peImage, coffOff + 16);
        int optHeaderOff = coffOff + 20;

        if (optHeaderOff + optHeaderSize > peImage.length)
        {
            pe32Plus = false;
            entryPointRva = 0;
            imageBase = 0;
            sizeOfImage = 0;
            sectionAlignment = 1;
            fileAlignment = 1;
            return;
        }

        // Optional header magic at optHeaderOff
        int magic = readLittleEndian16(peImage, optHeaderOff);
        pe32Plus = (magic == 0x20B); // PE32+

        if (pe32Plus)
        {
            // PE32+ optional header
            entryPointRva = readLittleEndian32(peImage, optHeaderOff + 16);
            imageBase = readInt64AsInt(peImage, optHeaderOff + 24); // truncate to 32-bit
            sizeOfImage = readLittleEndian32(peImage, optHeaderOff + 60);
            sectionAlignment = readLittleEndian32(peImage, optHeaderOff + 36);
            fileAlignment = readLittleEndian32(peImage, optHeaderOff + 40);
        }
        else
        {
            // PE32 optional header
            entryPointRva = readLittleEndian32(peImage, optHeaderOff + 16);
            imageBase = readLittleEndian32(peImage, optHeaderOff + 28);
            sizeOfImage = readLittleEndian32(peImage, optHeaderOff + 60);
            sectionAlignment = readLittleEndian32(peImage, optHeaderOff + 36);
            fileAlignment = readLittleEndian32(peImage, optHeaderOff + 40);
        }

        // Section headers start right after optional header
        int sectionTableOff = optHeaderOff + optHeaderSize;
        sections = new PeSection[numSections];
        for (int i = 0; i < numSections; i++)
        {
            int sOff = sectionTableOff + i * 40;
            if (sOff + 40 > peImage.length)
            {
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

    public PeSection[] getSections()
    {
        return sections;
    }

    public int getEntryPoint()
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
     * Reads an 8-byte little-endian field and returns its low 32 bits.
     * PE32+ ImageBase is 64-bit but we simulate a 32-bit flat space.
     */
    private static int readInt64AsInt(byte[] buf, int off)
    {
        return readLittleEndian32(buf, off);
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

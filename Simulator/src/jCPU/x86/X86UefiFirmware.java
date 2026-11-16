package jCPU.x86;

/**
 * Minimal UEFI firmware implementation.
 *
 * <p>The firmware is the first code that runs when the x86 simulator boots.
 * It performs the UEFI boot sequence:</p>
 *
 * <ul>
 *   <li>Installs a minimal UEFI System Table into simulated memory</li>
 *   <li>Reads the GPT from the IDE primary channel to find the EFI System
 *       Partition (ESP)</li>
 *   <li>Uses the FAT filesystem reader to load {@code \EFI\BOOT\BOOTIA32.EFI}
 *       from the ESP</li>
 *   <li>Loads the PE32 image into simulated memory; PE32+ (x64) images are
 *       loaded and served by {@link X86Uefi64}</li>
 *   <li>Transfers control to the loaded EFI application entry point</li>
 * </ul>
 *
 * <p>Console output is provided through both the UART (COM1) and the VGA
 * text buffer, matching the UEFI {@code Simple Text Output} protocol.</p>
 */
public class X86UefiFirmware
{
    private static final int SYSTEM_TABLE_ADDR = 0x00007000;
    private static final int BOOT_SERVICES_ADDR = 0x00007800;
    private static final int RUNTIME_SERVICES_ADDR = 0x00008000;
    private static final int CON_OUT_ADDR = 0x00008800;

    private static final int EFI_SUCCESS = 0;
    private static final int EFI_LOAD_ERROR = (int) 0x80000001L;

    private final X86Core core;
    private final X86IdeController ide;
    private final X86UartPeripheral uart;
    private final X86VgaPeripheral vga;

    private int vgaCursorX;
    private int vgaCursorY;

    private boolean initialized;
    private boolean lastBootPe32Plus;
    private X86Uefi64 x64;

    public X86UefiFirmware(X86Core core, X86IdeController ide,
                           X86UartPeripheral uart, X86VgaPeripheral vga)
    {
        this.core = core;
        this.ide = ide;
        this.uart = uart;
        this.vga = vga;
    }

    /**
     * Perform the UEFI boot sequence.  Returns the entry-point address of the
     * loaded EFI application, or 0 if no bootable image was found.
     *
     * <p>Call this from the simulator startup before starting execution; the
     * caller should then set {@code cpu.setPC(entryPoint)} and begin stepping.</p>
     */
    public long boot()
    {
        println("UEFI Firmware");
        println("=============");

        // Locate EFI System Partition via GPT
        X86FatFileSystem.SectorReader sectorReader = new X86FatFileSystem.SectorReader()
        {
            @Override
            public byte[] readSector(int lba)
            {
                return readIdeSector(lba);
            }
        };

        X86Gpt gpt = new X86Gpt(sectorReader);
        if (!gpt.parse(1))
        {
            println("GPT not found on IDE disk; trying MBR...");
            long mbrEspLba = tryMbrBoot(sectorReader);
            if (mbrEspLba < 0)
            {
                println("ERROR: No bootable MBR partition found.");
                return 0;
            }
            // Mount FAT on the MBR partition
            X86FatFileSystem fat = new X86FatFileSystem(sectorReader);
            if (!fat.mount((int) mbrEspLba))
            {
                println("ERROR: FAT filesystem not found on MBR partition at LBA " + mbrEspLba + ".");
                return 0;
            }
            println("FAT" + fat.getFatType() + " filesystem mounted on MBR partition.");
            return loadEfiFromFat(fat);
        }

        X86Gpt.Partition esp = gpt.findEfiSystemPartition();
        if (esp == null)
        {
            println("ERROR: EFI System Partition not found.");
            printPartitions(gpt);
            return 0;
        }

        println("Found ESP at LBA " + esp.firstLba + " (" + esp.name + ")");

        // Mount FAT filesystem on the ESP
        X86FatFileSystem fat = new X86FatFileSystem(sectorReader);
        // The ESP starts at esp.firstLba; the FAT BPB is in its first sector
        if (!fat.mount(esp.firstLba))
        {
            println("ERROR: FAT filesystem not found on ESP.");
            return 0;
        }
        println("FAT" + fat.getFatType() + " filesystem mounted.");

        return loadEfiFromFat(fat);
    }

    /* ---------------- MBR fallback ---------------- */

    /**
     * Try to boot from an MBR-partitioned disk.  Reads LBA 0, checks for the
     * 0x55AA signature, parses the partition table, and returns the first LBA
     * of a FAT-type partition, or -1 if none found.
     */
    private long tryMbrBoot(X86FatFileSystem.SectorReader sectorReader)
    {
        byte[] mbr = sectorReader.readSector(0);
        if (mbr == null || mbr.length < 512)
        {
            return -1;
        }

        // MBR signature at offset 510-511
        if ((mbr[510] & 0xFF) != 0x55 || (mbr[511] & 0xFF) != 0xAA)
        {
            return -1;
        }

        // FAT partition types
        int[] fatTypes = { 0x0B, 0x0C, 0x0E, 0x1B, 0x1C, 0xEB };

        // 4 partition entries at offset 446, 16 bytes each
        for (int i = 0; i < 4; i++)
        {
            int off = 446 + i * 16;
            int type = mbr[off + 4] & 0xFF;
            for (int fatType : fatTypes)
            {
                if (type == fatType)
                {
                    int firstLba = readLittleEndian32(mbr, off + 8);
                    int totalSectors = readLittleEndian32(mbr, off + 12);
                    println("MBR partition " + i + ": type=0x" + Integer.toHexString(type)
                            + " firstLBA=" + firstLba + " sectors=" + totalSectors);
                    if (firstLba > 0)
                    {
                        return firstLba;
                    }
                }
            }
        }
        return -1;
    }

    private long loadEfiFromFat(X86FatFileSystem fat)
    {
        // Try standard UEFI boot paths (IA32 and x64)
        String[] efiPaths = {
            "EFI\\BOOT\\BOOTIA32.EFI",
            "EFI/Boot/bootia32.efi",
            "EFI\\BOOT\\BOOTX64.EFI",
            "EFI/Boot/bootx64.efi",
            "EFI\\BOOT\\BOOT.EFI",
            "EFI/Boot/boot.efi"
        };

        X86FatFileSystem.FileEntry entry = null;
        String foundPath = null;
        for (String path : efiPaths)
        {
            entry = fat.findFile(path);
            if (entry != null)
            {
                foundPath = path;
                break;
            }
        }
        if (entry == null)
        {
            println("ERROR: No EFI boot image found. Tried:");
            for (String path : efiPaths)
            {
                println("  \\" + path);
            }
            // List root directory contents for debugging
            println("Root directory contents:");
            for (String e : fat.listRoot())
            {
                println("  " + e);
            }
            return 0;
        }

        println("Loading \\" + foundPath + " (" + entry.size + " bytes)...");
        byte[] peImage = fat.readFile(entry);
        if (peImage == null)
        {
            println("ERROR: Failed to read EFI image.");
            return 0;
        }

        X86PeLoader pe = new X86PeLoader(peImage);
        if (!pe.pe32Plus && pe.getSections().length == 0)
        {
            println("ERROR: Not a valid PE image.");
            return 0;
        }

        lastBootPe32Plus = pe.pe32Plus;

        long entryPoint;
        if (pe.pe32Plus)
        {
            x64 = new X86Uefi64(core, this::print, uart::pollInput);
            entryPoint = x64.load(pe, peImage);
        }
        else
        {
            // Install minimal UEFI data structures in memory
            installSystemTable();
            installedServices();
            entryPoint = loadPeImage(pe, peImage);
        }
        if (entryPoint == 0)
        {
            println("ERROR: Failed to load PE image into memory.");
            return 0;
        }

        println("Entry point at 0x" + Long.toHexString(entryPoint));
        println("Handing control to EFI application...");
        initialized = true;
        return entryPoint;
    }

    /** Returns true if the firmware has successfully booted an EFI application. */
    public boolean isInitialized()
    {
        return initialized;
    }

    /** Returns true if the last booted EFI application was a PE32+ (x64) image. */
    public boolean isPe32Plus()
    {
        return lastBootPe32Plus;
    }

    /** True once an x64 application returned from its entry point or called Exit(). */
    public boolean hasExited()
    {
        return x64 != null && x64.hasExited();
    }

    /** The EFI_STATUS the x64 application exited with. */
    public long getExitStatus()
    {
        return x64 == null ? 0 : x64.getExitStatus();
    }

    /* ---------------- UEFI data structures ---------------- */

    private void installSystemTable()
    {
        // UEFI System Table (simplified)
        // Offset 0:  Signature (8 bytes) "EFI ST\0\0"
        writeString(SYSTEM_TABLE_ADDR, "EFI ST\0\0");
        // Offset 8:  Revision (4 bytes)
        writeMem32(SYSTEM_TABLE_ADDR + 8, 0x00020000); // UEFI 2.0
        // Offset 12: HeaderSize (4 bytes)
        writeMem32(SYSTEM_TABLE_ADDR + 12, 96);
        // Offset 16: CRC32 (4 bytes)
        writeMem32(SYSTEM_TABLE_ADDR + 16, 0);
        // Offset 20: Reserved (4 bytes)
        writeMem32(SYSTEM_TABLE_ADDR + 20, 0);
        // Offset 24: FirmwareVendor (ptr)
        writeMem32(SYSTEM_TABLE_ADDR + 24, 0x00009000);
        writeString(0x00009000, "SimUEFI\0");
        // Offset 28: ConsoleInHandle
        writeMem32(SYSTEM_TABLE_ADDR + 28, 0);
        // Offset 32: ConIn
        writeMem32(SYSTEM_TABLE_ADDR + 32, 0);
        // Offset 36: ConsoleOutHandle
        writeMem32(SYSTEM_TABLE_ADDR + 36, 1);
        // Offset 40: ConOut
        writeMem32(SYSTEM_TABLE_ADDR + 40, CON_OUT_ADDR);
        // Offset 44: StandardErrorHandle
        writeMem32(SYSTEM_TABLE_ADDR + 44, 1);
        // Offset 48: StdErr
        writeMem32(SYSTEM_TABLE_ADDR + 48, CON_OUT_ADDR);
        // Offset 52: RuntimeServices
        writeMem32(SYSTEM_TABLE_ADDR + 52, RUNTIME_SERVICES_ADDR);
        // Offset 56: BootServices
        writeMem32(SYSTEM_TABLE_ADDR + 56, BOOT_SERVICES_ADDR);
        // Offset 60: NumberOfTableEntries
        writeMem32(SYSTEM_TABLE_ADDR + 60, 0);
        // Offset 64: ConfigurationTable
        writeMem32(SYSTEM_TABLE_ADDR + 64, 0);

        // Simple Text Output Protocol (ConOut)
        writeMem32(CON_OUT_ADDR + 0, 0);   // Reset (ptr)
        writeMem32(CON_OUT_ADDR + 4, 0);   // OutputString (ptr)
        writeMem32(CON_OUT_ADDR + 8, 0);   // TestString (ptr)
        writeMem32(CON_OUT_ADDR + 12, 0);  // QueryMode (ptr)
        writeMem32(CON_OUT_ADDR + 16, 0);  // SetMode (ptr)
        writeMem32(CON_OUT_ADDR + 20, 0);  // SetAttribute (ptr)
        writeMem32(CON_OUT_ADDR + 24, 0);  // ClearScreen (ptr)
        writeMem32(CON_OUT_ADDR + 28, 0);  // SetCursorPosition (ptr)
        writeMem32(CON_OUT_ADDR + 32, 0);  // EnableCursor (ptr)
        writeMem32(CON_OUT_ADDR + 36, 0);  // Mode (ptr)
    }

    private void installedServices()
    {
        // Minimal Boot Services and Runtime Services stubs installed in memory
        // so EFI applications can locate them.  Actual service calls from x86
        // code would require an interrupt/trap mechanism; here they exist as
        // discoverable data structures.
        writeMem32(BOOT_SERVICES_ADDR + 0, 0x00000000); // Header
        writeMem32(BOOT_SERVICES_ADDR + 4, 0x00020000); // Revision
        writeMem32(BOOT_SERVICES_ADDR + 8, 96);        // HeaderSize

        writeMem32(RUNTIME_SERVICES_ADDR + 0, 0);
        writeMem32(RUNTIME_SERVICES_ADDR + 4, 0x00020000);
        writeMem32(RUNTIME_SERVICES_ADDR + 8, 96);
    }

    /* ---------------- PE loading ---------------- */

    private long loadPeImage(X86PeLoader pe, byte[] peImage)
    {
        long imageBase = pe.imageBase;

        for (X86PeLoader.PeSection sec : pe.getSections())
        {
            if (sec.rawSize == 0)
            {
                continue;
            }
            long dest = imageBase + sec.virtualAddress;
            int copyLen = Math.min(sec.virtualSize, sec.rawSize);
            // Clamp to PE image bounds
            if (sec.rawAddress + copyLen > peImage.length)
            {
                copyLen = peImage.length - sec.rawAddress;
                if (copyLen <= 0) continue;
            }
            for (int i = 0; i < copyLen; i++)
            {
                core.writeMem8((int) dest, peImage[sec.rawAddress + i]);
                dest++;
            }
            // Zero-fill remainder of virtual size
            for (int i = copyLen; i < sec.virtualSize; i++)
            {
                core.writeMem8((int) (imageBase + sec.virtualAddress + i), 0);
            }
        }

        return pe.getEntryPoint64();
    }

    /* ---------------- IDE sector access ---------------- */

    private byte[] readIdeSector(int lba)
    {
        if (ide == null)
        {
            return null;
        }
        X86IdeDevice dev = ide.getMaster();
        if (dev == null)
        {
            return null;
        }
        byte[] buf = new byte[X86IdeDevice.SECTOR_SIZE];
        dev.readSector(lba, buf);
        return buf;
    }

    private static int readLittleEndian32(byte[] buf, int off)
    {
        return (buf[off] & 0xFF) | ((buf[off + 1] & 0xFF) << 8) |
               ((buf[off + 2] & 0xFF) << 16) | ((buf[off + 3] & 0xFF) << 24);
    }

    /* ---------------- Console output ---------------- */

    private void print(String s)
    {
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            uart.write(0x3F8, (byte) c);
            vgaWriteChar(c);
        }
    }

    private void println(String s)
    {
        print(s);
        print("\n");
    }

    private void vgaWriteChar(char c)
    {
        if (vga == null)
        {
            return;
        }
        if (c == '\r')
        {
            vgaCursorX = 0;
            return;
        }
        if (c == '\n')
        {
            vgaCursorX = 0;
            vgaCursorY++;
            if (vgaCursorY >= 25)
            {
                vgaCursorY = 0;
            }
            return;
        }
        int offset = (vgaCursorY * 80 + vgaCursorX) * 2;
        vga.writeMemory(0xB8000 + offset, (byte) c);
        vga.writeMemory(0xB8000 + offset + 1, 0x07); // light gray on black
        vgaCursorX++;
        if (vgaCursorX >= 80)
        {
            vgaCursorX = 0;
            vgaCursorY++;
            if (vgaCursorY >= 25)
            {
                vgaCursorY = 0;
            }
        }
    }

    /* ---------------- helpers ---------------- */

    private void writeMem32(int addr, int value)
    {
        core.writeMem32(addr, value);
    }

    private void writeString(int addr, String s)
    {
        for (int i = 0; i < s.length(); i++)
        {
            core.writeMem8(addr + i, (byte) s.charAt(i));
        }
        core.writeMem8(addr + s.length(), 0);
    }

    private void printPartitions(X86Gpt gpt)
    {
        println("Partitions:");
        for (X86Gpt.Partition p : gpt.getPartitions())
        {
            println("  " + p);
        }
    }
}

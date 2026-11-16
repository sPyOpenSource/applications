package jCPU.x86;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.zip.CRC32;

/**
 * x64 UEFI environment for a PE32+ application: the system table, boot and
 * runtime services and the console protocols, laid out as the UEFI
 * specification defines them for 64-bit code.
 *
 * <p>Every service pointer in the tables points into a trap range (see
 * {@link X86Core#setFirmwareTrap}); a call there runs the Java method for
 * that service with the Microsoft x64 calling convention (arguments in RCX,
 * RDX, R8, R9, then on the stack; status in RAX).  Services that are not
 * implemented print their name and return {@code EFI_UNSUPPORTED}, so a run
 * shows which one an application needs next.</p>
 *
 * <p>Memory layout (all below 64 MB):</p>
 * <pre>
 *   0x00100000  tables, strings, handles, protocol interfaces, device paths
 *   0x00101000  trap range, 8 bytes per service
 *   0x00400000  stack (grows down from 0x00800000)
 *   0x01000000  the application, unless its own ImageBase is free
 * </pre>
 *
 * <p>All other memory is free for AllocatePages/AllocatePool, which take it
 * from the top down; {@link X86UefiMemoryMap} keeps track and is what
 * GetMemoryMap returns.</p>
 */
public class X86Uefi64
{
    static final long EFI_SUCCESS = 0;
    static final long EFI_INVALID_PARAMETER = 0x8000000000000002L;
    static final long EFI_UNSUPPORTED = 0x8000000000000003L;
    static final long EFI_BAD_BUFFER_SIZE = 0x8000000000000004L;
    static final long EFI_BUFFER_TOO_SMALL = 0x8000000000000005L;
    static final long EFI_NOT_READY = 0x8000000000000006L;
    static final long EFI_WRITE_PROTECTED = 0x8000000000000008L;
    static final long EFI_OUT_OF_RESOURCES = 0x8000000000000009L;
    static final long EFI_NOT_FOUND = 0x800000000000000EL;

    static final long FIRMWARE = 0x00100000L;
    static final long SYSTEM_TABLE = FIRMWARE;
    static final long BOOT_SERVICES = FIRMWARE + 0x100;
    static final long RUNTIME_SERVICES = FIRMWARE + 0x300;
    static final long CON_OUT = FIRMWARE + 0x400;
    static final long CON_OUT_MODE = FIRMWARE + 0x480;
    static final long CON_IN = FIRMWARE + 0x500;
    static final long VENDOR = FIRMWARE + 0x600;
    static final long IMAGE_HANDLE = FIRMWARE + 0x700;
    static final long CON_IN_HANDLE = FIRMWARE + 0x710;
    static final long CON_OUT_HANDLE = FIRMWARE + 0x720;
    static final long DISK_HANDLE = FIRMWARE + 0x730;
    static final long PARTITION_HANDLE = FIRMWARE + 0x740;
    static final long LOADED_IMAGE = FIRMWARE + 0x800;
    /** Device paths and BlockIo structures are placed from here up to TRAPS. */
    static final long FIRMWARE_DATA = FIRMWARE + 0x900;
    static final long TRAPS = FIRMWARE + 0x1000;
    static final int MAX_SERVICES = 256;
    static final long TSC_PER_MICROSECOND = X86Core.TSC_HZ / 1_000_000;
    static final long STACK_TOP = 0x00800000L;
    static final long IMAGE_LOAD = 0x01000000L;
    /** X86Core always backs 64 MB, whatever size it was given. */
    static final long MEMORY_END = 64L * 1024 * 1024;

    /** Boot services in table order, after the 24-byte header. */
    private static final String[] BOOT_SERVICE_NAMES = {
        "RaiseTPL", "RestoreTPL", "AllocatePages", "FreePages", "GetMemoryMap",
        "AllocatePool", "FreePool", "CreateEvent", "SetTimer", "WaitForEvent",
        "SignalEvent", "CloseEvent", "CheckEvent", "InstallProtocolInterface",
        "ReinstallProtocolInterface", "UninstallProtocolInterface", "HandleProtocol",
        "Reserved", "RegisterProtocolNotify", "LocateHandle", "LocateDevicePath",
        "InstallConfigurationTable", "LoadImage", "StartImage", "Exit", "UnloadImage",
        "ExitBootServices", "GetNextMonotonicCount", "Stall", "SetWatchdogTimer",
        "ConnectController", "DisconnectController", "OpenProtocol", "CloseProtocol",
        "OpenProtocolInformation", "ProtocolsPerHandle", "LocateHandleBuffer",
        "LocateProtocol", "InstallMultipleProtocolInterfaces",
        "UninstallMultipleProtocolInterfaces", "CalculateCrc32", "CopyMem", "SetMem",
        "CreateEventEx"
    };

    private static final String[] RUNTIME_SERVICE_NAMES = {
        "GetTime", "SetTime", "GetWakeupTime", "SetWakeupTime", "SetVirtualAddressMap",
        "ConvertPointer", "GetVariable", "GetNextVariableName", "SetVariable",
        "GetNextHighMonotonicCount", "ResetSystem", "UpdateCapsule",
        "QueryCapsuleCapabilities", "QueryVariableInfo"
    };

    private static final String[] CON_OUT_NAMES = {
        "Reset", "OutputString", "TestString", "QueryMode", "SetMode", "SetAttribute",
        "ClearScreen", "SetCursorPosition", "EnableCursor"
    };

    private static final String[] CON_IN_NAMES = { "Reset", "ReadKeyStroke" };

    /** A firmware service; returns the EFI_STATUS for RAX. */
    interface Service {
        long call(X86Core core, long[] args) throws Exception;
    }

    private static final class Entry {
        final String name;
        final Service service;

        Entry(String name, Service service) {
            this.name = name;
            this.service = service;
        }
    }

    private final X86Core core;
    private final Consumer<String> console;
    private final IntSupplier keyboard;
    private final List<Entry> services = new ArrayList<>();
    private X86UefiMemoryMap memoryMap;
    /** Pages behind each AllocatePool buffer, for FreePool. */
    private final Map<Long, Long> pools = new HashMap<>();
    /** Handle -> protocol GUID -> interface. */
    private final Map<Long, Map<String, Long>> handles = new LinkedHashMap<>();
    /** BlockIo interface -> {first sector on the disk, last block}. */
    private final Map<Long, long[]> blockDevices = new HashMap<>();
    private BootDisk bootDisk;
    private long firmwareDataNext;

    /** The disk and partition the application is loaded from. */
    public static final class BootDisk
    {
        final X86FatFileSystem.SectorReader reader;
        final long sectors;
        final int partitionNumber;
        final long partitionStart;
        final long partitionSize;
        final byte[] signature;         // 16 bytes: GPT unique GUID or MBR disk signature
        final int mbrType;              // 1 = MBR, 2 = GPT
        final String filePath;          // e.g. \EFI\BOOT\BOOTX64.EFI

        private BootDisk(X86FatFileSystem.SectorReader reader, long sectors, int number,
                         long start, long size, byte[] signature, int mbrType, String filePath)
        {
            this.reader = reader;
            this.sectors = sectors;
            this.partitionNumber = number;
            this.partitionStart = start;
            this.partitionSize = size;
            this.signature = signature;
            this.mbrType = mbrType;
            this.filePath = "\\" + filePath.replace('/', '\\').replaceFirst("^\\\\+", "");
        }

        public static BootDisk gpt(X86FatFileSystem.SectorReader reader, long sectors,
                                   X86Gpt.Partition p, String filePath)
        {
            return new BootDisk(reader, sectors, p.number, p.firstLba,
                    (long) p.lastLba - p.firstLba + 1, guidBytes(p.uniqueGuid), 2, filePath);
        }

        public static BootDisk mbr(X86FatFileSystem.SectorReader reader, long sectors, int number,
                                   long start, long size, int diskSignature, String filePath)
        {
            byte[] sig = new byte[16];
            for (int i = 0; i < 4; i++) {
                sig[i] = (byte) (diskSignature >>> (8 * i));
            }
            return new BootDisk(reader, sectors, number, start, size, sig, 1, filePath);
        }
    }

    /** Offer this disk to the application (BlockIo, DevicePath) and boot from its partition. */
    public void attachBootDisk(BootDisk disk)
    {
        this.bootDisk = disk;
    }
    private boolean exited;
    private boolean atLineStart = true;
    private long exitStatus;

    /**
     * @param console  receives everything the application prints
     * @param keyboard the next typed character, or -1 when there is none
     */
    public X86Uefi64(X86Core core, Consumer<String> console, IntSupplier keyboard)
    {
        this.core = core;
        this.console = console;
        this.keyboard = keyboard;
    }

    /**
     * Load the application, build the tables and set up the registers for
     * its entry: RCX = ImageHandle, RDX = SystemTable, and a return address
     * on the stack that ends the run.
     *
     * @return the entry point, or 0 if the image cannot be placed in memory
     */
    public long load(X86PeLoader pe, byte[] peImage)
    {
        memoryMap = new X86UefiMemoryMap(MEMORY_END);
        memoryMap.set(0, X86UefiMemoryMap.PAGE, X86UefiMemoryMap.RESERVED);  // keep null pointers invalid
        memoryMap.set(0xA0000, FIRMWARE, X86UefiMemoryMap.RESERVED);         // VGA and BIOS area
        memoryMap.set(FIRMWARE, TRAPS, X86UefiMemoryMap.BOOT_SERVICES_DATA);
        memoryMap.set(TRAPS, TRAPS + MAX_SERVICES * 8, X86UefiMemoryMap.RUNTIME_SERVICES_CODE);
        memoryMap.set(STACK_TOP - 0x400000, STACK_TOP, X86UefiMemoryMap.BOOT_SERVICES_DATA);

        long pages = (pe.sizeOfImage + X86UefiMemoryMap.PAGE - 1) / X86UefiMemoryMap.PAGE;
        long base = IMAGE_LOAD;
        if (pe.imageBase % X86UefiMemoryMap.PAGE == 0 && pe.imageBase + pe.sizeOfImage <= MEMORY_END
                && memoryMap.isFree(pe.imageBase, pages)) {
            base = pe.imageBase;
        }
        if (!memoryMap.isFree(base, pages)) {
            message("ERROR: image of " + pe.sizeOfImage + " bytes does not fit at 0x"
                    + Long.toHexString(base) + ".");
            return 0;
        }
        memoryMap.set(base, base + pe.sizeOfImage, X86UefiMemoryMap.LOADER_CODE);
        byte[] image = pe.layout(peImage, base);
        if (image == null) {
            message("ERROR: image must be loaded at 0x" + Long.toHexString(pe.imageBase)
                    + ", which is not in memory, and its relocations were stripped.");
            return 0;
        }
        for (int i = 0; i < image.length; i++) {
            core.writeMem8(base + i, image[i]);
        }
        if (base != pe.imageBase) {
            message("Relocated from 0x" + Long.toHexString(pe.imageBase)
                    + " to 0x" + Long.toHexString(base) + ".");
        }

        services.clear();
        // Trap 0 is the return address of the entry point.
        services.add(new Entry("return from entry point", (c, a) -> exit(c.getReg64(X86Core.REG_RAX))));
        for (long a = TRAPS; a < TRAPS + MAX_SERVICES * 8; a++) {
            core.writeMem8(a, 0xF4);    // HLT, if anything reads the trap range as code
        }
        installTables();
        installProtocols(base, pe.sizeOfImage);
        core.setFirmwareTrap(TRAPS, TRAPS + MAX_SERVICES * 8, this::trap);

        core.setReg64(X86Core.REG_RSP, STACK_TOP);
        core.push64(TRAPS);             // RSP is now 16n + 8, as after a call
        core.setReg64(X86Core.REG_RCX, IMAGE_HANDLE);
        core.setReg64(X86Core.REG_RDX, SYSTEM_TABLE);
        exited = false;
        return base + pe.entryPointRva;
    }

    /** True once the application returned from its entry point or called Exit(). */
    public boolean hasExited()
    {
        return exited;
    }

    public long getExitStatus()
    {
        return exitStatus;
    }

    /* ---------------- tables ---------------- */

    private void installTables()
    {
        writeUcs2(VENDOR, "SimUEFI");

        header(SYSTEM_TABLE, 0x5453595320494249L, 120);         // "IBI SYST"
        core.writeMem64(SYSTEM_TABLE + 24, VENDOR);
        core.writeMem32(SYSTEM_TABLE + 32, 0x00010000);          // firmware revision
        core.writeMem64(SYSTEM_TABLE + 40, CON_IN_HANDLE);
        core.writeMem64(SYSTEM_TABLE + 48, CON_IN);
        core.writeMem64(SYSTEM_TABLE + 56, CON_OUT_HANDLE);
        core.writeMem64(SYSTEM_TABLE + 64, CON_OUT);
        core.writeMem64(SYSTEM_TABLE + 72, CON_OUT_HANDLE);
        core.writeMem64(SYSTEM_TABLE + 80, CON_OUT);
        core.writeMem64(SYSTEM_TABLE + 88, RUNTIME_SERVICES);
        core.writeMem64(SYSTEM_TABLE + 96, BOOT_SERVICES);
        core.writeMem64(SYSTEM_TABLE + 104, 0);                  // configuration tables
        core.writeMem64(SYSTEM_TABLE + 112, 0);

        header(BOOT_SERVICES, 0x56524553544F4F42L, 24 + BOOT_SERVICE_NAMES.length * 8); // "BOOTSERV"
        functions(BOOT_SERVICES + 24, "BootServices", BOOT_SERVICE_NAMES);
        header(RUNTIME_SERVICES, 0x56524553544E5552L, 24 + RUNTIME_SERVICE_NAMES.length * 8); // "RUNTSERV"
        functions(RUNTIME_SERVICES + 24, "RuntimeServices", RUNTIME_SERVICE_NAMES);

        functions(CON_OUT, "ConOut", CON_OUT_NAMES);
        core.writeMem64(CON_OUT + 72, CON_OUT_MODE);
        core.writeMem32(CON_OUT_MODE, 1);                        // MaxMode
        core.writeMem32(CON_OUT_MODE + 4, 0);                    // Mode
        core.writeMem32(CON_OUT_MODE + 8, 0x07);                 // Attribute
        core.writeMem32(CON_OUT_MODE + 12, 0);                   // CursorColumn
        core.writeMem32(CON_OUT_MODE + 16, 0);                   // CursorRow
        core.writeMem8(CON_OUT_MODE + 20, 1);                    // CursorVisible

        functions(CON_IN, "ConIn", CON_IN_NAMES);
        core.writeMem64(CON_IN + 16, 0);                         // WaitForKey event

        crc(SYSTEM_TABLE);
        crc(BOOT_SERVICES);
        crc(RUNTIME_SERVICES);
    }

    private void header(long table, long signature, int size)
    {
        core.writeMem64(table, signature);
        core.writeMem32(table + 8, (2 << 16) | 70);              // UEFI 2.70
        core.writeMem32(table + 12, size);
        core.writeMem32(table + 16, 0);
        core.writeMem32(table + 20, 0);
    }

    private void crc(long table)
    {
        int size = core.readMem32(table + 12);
        CRC32 crc = new CRC32();
        for (int i = 0; i < size; i++) {
            crc.update(core.readMem8(table + i));
        }
        core.writeMem32(table + 16, (int) crc.getValue());
    }

    /** Fill a table of function pointers, one trap per name. */
    private void functions(long table, String owner, String[] names)
    {
        for (int i = 0; i < names.length; i++) {
            String name = owner + "." + names[i];
            core.writeMem64(table + 8L * i, service(name, implementation(name)));
        }
    }

    private long service(String name, Service s)
    {
        if (services.size() >= MAX_SERVICES) {
            throw new IllegalStateException("too many firmware services");
        }
        services.add(new Entry(name, s));
        return TRAPS + 8L * (services.size() - 1);
    }

    /* ---------------- handles and protocols ---------------- */

    static final String LOADED_IMAGE_PROTOCOL = "5b1b31a1-9562-11d2-8e3f-00a0c969723b";
    static final String SIMPLE_TEXT_INPUT_PROTOCOL = "387477c1-69c7-11d2-8e39-00a0c969723b";
    static final String SIMPLE_TEXT_OUTPUT_PROTOCOL = "387477c2-69c7-11d2-8e39-00a0c969723b";

    /** Names for log messages. */
    private static final Map<String, String> GUID_NAMES = new HashMap<>();
    static {
        GUID_NAMES.put(LOADED_IMAGE_PROTOCOL, "LoadedImage");
        GUID_NAMES.put(SIMPLE_TEXT_INPUT_PROTOCOL, "SimpleTextInput");
        GUID_NAMES.put(SIMPLE_TEXT_OUTPUT_PROTOCOL, "SimpleTextOutput");
        GUID_NAMES.put("09576e91-6d3f-11d2-8e39-00a0c969723b", "DevicePath");
        GUID_NAMES.put("bc62157e-3e33-4fec-9920-2d3b36d750df", "LoadedImageDevicePath");
        GUID_NAMES.put("964e5b21-6459-11d2-8e39-00a0c969723b", "BlockIo");
        GUID_NAMES.put("ce345171-ba0b-11d2-8e4f-00a0c969723b", "DiskIo");
        GUID_NAMES.put("964e5b22-6459-11d2-8e39-00a0c969723b", "SimpleFileSystem");
        GUID_NAMES.put("9042a9de-23dc-4a38-96fb-7aded080516a", "GraphicsOutput");
        GUID_NAMES.put("dd9e7534-7762-4698-8c14-f58517a625aa", "SimpleTextInputEx");
        GUID_NAMES.put("f42f7782-012e-4c12-9956-49f94304f721", "ConsoleControl");
        GUID_NAMES.put("605dab50-e046-4300-abb6-3dd810dd8b23", "ShimLock");
        GUID_NAMES.put("607f766c-7455-42be-930b-e4d76db2720f", "Tcg2");
        GUID_NAMES.put("f541796d-a62e-4954-a775-9584f61b9cdd", "Tcg");
        GUID_NAMES.put("3152bca5-eade-433d-862e-c01cdc291f44", "Rng");
        GUID_NAMES.put("8b843e20-8132-4852-90cc-551a4e4a7f1c", "DevicePathToText");
        GUID_NAMES.put("bb25cf6f-f1d4-11d2-9a0c-0090273fc1fd", "SerialIo");
    }

    private void installProtocols(long imageBase, long imageSize)
    {
        handles.clear();
        install(CON_IN_HANDLE, SIMPLE_TEXT_INPUT_PROTOCOL, CON_IN);
        install(CON_OUT_HANDLE, SIMPLE_TEXT_OUTPUT_PROTOCOL, CON_OUT);

        // EFI_LOADED_IMAGE_PROTOCOL
        core.writeMem32(LOADED_IMAGE, 0x1000);                  // Revision
        core.writeMem64(LOADED_IMAGE + 8, 0);                   // ParentHandle
        core.writeMem64(LOADED_IMAGE + 16, SYSTEM_TABLE);
        core.writeMem64(LOADED_IMAGE + 24, 0);                  // DeviceHandle
        core.writeMem64(LOADED_IMAGE + 32, 0);                  // FilePath
        core.writeMem64(LOADED_IMAGE + 40, 0);                  // Reserved
        core.writeMem32(LOADED_IMAGE + 48, 0);                  // LoadOptionsSize
        core.writeMem64(LOADED_IMAGE + 56, 0);                  // LoadOptions
        core.writeMem64(LOADED_IMAGE + 64, imageBase);
        core.writeMem64(LOADED_IMAGE + 72, imageSize);
        core.writeMem32(LOADED_IMAGE + 80, X86UefiMemoryMap.LOADER_CODE);
        core.writeMem32(LOADED_IMAGE + 84, X86UefiMemoryMap.LOADER_DATA);
        core.writeMem64(LOADED_IMAGE + 88, 0);                  // Unload
        install(IMAGE_HANDLE, LOADED_IMAGE_PROTOCOL, LOADED_IMAGE);

        firmwareDataNext = FIRMWARE_DATA;
        blockDevices.clear();
        if (bootDisk != null) {
            installDisk(bootDisk);
        }
    }

    static final String DEVICE_PATH_PROTOCOL = "09576e91-6d3f-11d2-8e39-00a0c969723b";
    static final String BLOCK_IO_PROTOCOL = "964e5b21-6459-11d2-8e39-00a0c969723b";

    /**
     * Two handles, as real firmware has them: the whole disk and the boot
     * partition, each with a DevicePath and a BlockIo.  The partition's path
     * is the disk's plus a HardDrive node, which is how applications such as
     * GRUB find out which disk and partition they were loaded from.
     */
    private void installDisk(BootDisk d)
    {
        byte[] acpi = node(2, 1, 0xD0, 0x41, 0x03, 0x0A, 0, 0, 0, 0);    // PciRoot: PNP0A03, UID 0
        byte[] pci = node(1, 1, 1, 1);                                    // function 1, device 1: IDE
        byte[] ata = node(3, 1, 0, 0, 0, 0);                              // primary, master, LUN 0
        byte[] hd = new byte[42];
        hd[0] = 4;                                                        // media
        hd[1] = 1;                                                        // hard drive
        hd[2] = 42;
        putLE(hd, 4, d.partitionNumber, 4);
        putLE(hd, 8, d.partitionStart, 8);
        putLE(hd, 16, d.partitionSize, 8);
        System.arraycopy(d.signature, 0, hd, 24, 16);
        hd[40] = (byte) d.mbrType;
        hd[41] = (byte) d.mbrType;                                        // signature type: same numbering

        long diskPath = devicePath(acpi, pci, ata);
        long partPath = devicePath(acpi, pci, ata, hd);
        long diskIo = blockIo(0, d.sectors - 1, false, 0);
        long partIo = blockIo(d.partitionStart, d.partitionSize - 1, true, diskIo);
        install(DISK_HANDLE, DEVICE_PATH_PROTOCOL, diskPath);
        install(DISK_HANDLE, BLOCK_IO_PROTOCOL, diskIo);
        install(PARTITION_HANDLE, DEVICE_PATH_PROTOCOL, partPath);
        install(PARTITION_HANDLE, BLOCK_IO_PROTOCOL, partIo);

        // The loaded image came from the partition; FilePath is relative to it.
        byte[] path = new byte[4 + 2 * (d.filePath.length() + 1)];
        path[0] = 4;                                                      // media
        path[1] = 4;                                                      // file path
        putLE(path, 2, path.length, 2);
        for (int i = 0; i < d.filePath.length(); i++) {
            putLE(path, 4 + 2 * i, d.filePath.charAt(i), 2);
        }
        core.writeMem64(LOADED_IMAGE + 24, PARTITION_HANDLE);
        core.writeMem64(LOADED_IMAGE + 32, devicePath(path));
    }

    private static byte[] node(int type, int subtype, int... data)
    {
        byte[] n = new byte[4 + data.length];
        n[0] = (byte) type;
        n[1] = (byte) subtype;
        n[2] = (byte) n.length;
        for (int i = 0; i < data.length; i++) {
            n[4 + i] = (byte) data[i];
        }
        return n;
    }

    private static void putLE(byte[] b, int off, long v, int size)
    {
        for (int i = 0; i < size; i++) {
            b[off + i] = (byte) (v >>> (8 * i));
        }
    }

    /** Space in the firmware data area, 8-byte aligned. */
    private long firmwareData(int size)
    {
        long p = (firmwareDataNext + 7) & -8L;
        if (p + size > TRAPS) {
            throw new IllegalStateException("firmware data area is full");
        }
        firmwareDataNext = p + size;
        return p;
    }

    /** Write the nodes followed by an End node; returns the address. */
    private long devicePath(byte[]... nodes)
    {
        int size = 4;
        for (byte[] n : nodes) {
            size += n.length;
        }
        long p = firmwareData(size);
        long a = p;
        for (byte[] n : nodes) {
            for (byte b : n) {
                core.writeMem8(a++, b);
            }
        }
        core.writeMem8(a, 0x7F);
        core.writeMem8(a + 1, 0xFF);
        core.writeMem16(a + 2, 4);
        return p;
    }

    /**
     * EFI_BLOCK_IO_PROTOCOL (revision 1) with its media, read-only, 512-byte
     * blocks.  The function pointers are shared: the interface address tells
     * which device a call is for.
     */
    private long blockIo(long firstSector, long lastBlock, boolean partition, long sharedFunctions)
    {
        long media = firmwareData(32);
        core.writeMem32(media, 0);                      // MediaId
        core.writeMem8(media + 4, 0);                   // RemovableMedia
        core.writeMem8(media + 5, 1);                   // MediaPresent
        core.writeMem8(media + 6, partition ? 1 : 0);   // LogicalPartition
        core.writeMem8(media + 7, 1);                   // ReadOnly
        core.writeMem8(media + 8, 0);                   // WriteCaching
        core.writeMem32(media + 12, X86IdeDevice.SECTOR_SIZE);
        core.writeMem32(media + 16, 0);                 // IoAlign
        core.writeMem64(media + 24, lastBlock);

        long bio = firmwareData(48);
        core.writeMem64(bio, 0x00010000);               // Revision
        core.writeMem64(bio + 8, media);
        if (sharedFunctions == 0) {
            functions(bio + 16, "BlockIo", BLOCK_IO_NAMES);
        } else {
            for (int i = 0; i < BLOCK_IO_NAMES.length; i++) {
                core.writeMem64(bio + 16 + 8L * i, core.readMem64(sharedFunctions + 16 + 8L * i));
            }
        }
        blockDevices.put(bio, new long[] { firstSector, lastBlock });
        return bio;
    }

    private static final String[] BLOCK_IO_NAMES = { "Reset", "ReadBlocks", "WriteBlocks", "FlushBlocks" };

    /** ReadBlocks(This, MediaId, Lba, BufferSize, Buffer). */
    private long readBlocks(X86Core c, long[] a)
    {
        long[] dev = blockDevices.get(a[0]);
        if (dev == null) {
            return EFI_INVALID_PARAMETER;
        }
        long lba = a[2];
        long size = a[3];
        long buffer = a[4];
        int sector = X86IdeDevice.SECTOR_SIZE;
        if (size % sector != 0) {
            return EFI_BAD_BUFFER_SIZE;
        }
        long count = size / sector;
        if (lba < 0 || lba + count - 1 > dev[1] || buffer == 0) {
            return EFI_INVALID_PARAMETER;
        }
        for (long i = 0; i < count; i++) {
            byte[] data = bootDisk.reader.readSector((int) (dev[0] + lba + i));
            for (int k = 0; k < sector; k++) {
                c.writeMem8(buffer + i * sector + k, data == null ? 0 : data[k]);
            }
        }
        return EFI_SUCCESS;
    }

    /**
     * LocateHandle(SearchType, Protocol, SearchKey, *BufferSize, Buffer):
     * AllHandles (0) or ByProtocol (2).
     */
    private long locateHandle(X86Core c, long[] a)
    {
        List<Long> found = handlesFor((int) a[0], a[1]);
        if (found.isEmpty()) {
            return EFI_NOT_FOUND;
        }
        long need = 8L * found.size();
        long have = c.readMem64(a[3]);
        c.writeMem64(a[3], need);
        if (have < need) {
            return EFI_BUFFER_TOO_SMALL;
        }
        for (int i = 0; i < found.size(); i++) {
            c.writeMem64(a[4] + 8L * i, found.get(i));
        }
        return EFI_SUCCESS;
    }

    /** LocateHandleBuffer(SearchType, Protocol, SearchKey, *NoHandles, **Buffer). */
    private long locateHandleBuffer(X86Core c, long[] a)
    {
        List<Long> found = handlesFor((int) a[0], a[1]);
        if (found.isEmpty()) {
            return EFI_NOT_FOUND;
        }
        long status = allocatePool(c, X86UefiMemoryMap.BOOT_SERVICES_DATA, 8L * found.size(), a[4]);
        if (status != EFI_SUCCESS) {
            return status;
        }
        long buffer = c.readMem64(a[4]);
        for (int i = 0; i < found.size(); i++) {
            c.writeMem64(buffer + 8L * i, found.get(i));
        }
        c.writeMem64(a[3], found.size());
        return EFI_SUCCESS;
    }

    private List<Long> handlesFor(int searchType, long guidPtr)
    {
        List<Long> found = new ArrayList<>();
        if (searchType == 0) {
            found.addAll(handles.keySet());
        } else if (searchType == 2) {
            String guid = guid(core, guidPtr);
            for (Map.Entry<Long, Map<String, Long>> h : handles.entrySet()) {
                if (h.getValue().containsKey(guid)) {
                    found.add(h.getKey());
                }
            }
            if (found.isEmpty()) {
                message("[UEFI] LocateHandle(" + guidName(guid) + "): none");
            }
        }
        return found;
    }

    /** "C12A7328-F81F-11D2-BA4B-00A0C93EC93B" to its 16 bytes in memory order. */
    static byte[] guidBytes(String text)
    {
        String hex = text.replace("-", "");
        byte[] raw = new byte[16];
        for (int i = 0; i < 16; i++) {
            raw[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        byte[] b = raw.clone();
        int[] order = { 3, 2, 1, 0, 5, 4, 7, 6 };       // Data1, Data2, Data3 are little endian
        for (int i = 0; i < 8; i++) {
            b[i] = raw[order[i]];
        }
        return b;
    }

    private void install(long handle, String guid, long iface)
    {
        handles.computeIfAbsent(handle, h -> new LinkedHashMap<>()).put(guid, iface);
    }

    /** The EFI_GUID at {@code addr} in its usual text form. */
    static String guid(X86Core c, long addr)
    {
        StringBuilder sb = new StringBuilder(String.format("%08x-%04x-%04x-",
                c.readMem32(addr), c.readMem16(addr + 4), c.readMem16(addr + 6)));
        for (int i = 8; i < 16; i++) {
            sb.append(String.format("%02x", c.readMem8(addr + i)));
            if (i == 9) {
                sb.append('-');
            }
        }
        return sb.toString();
    }

    private static String guidName(String guid)
    {
        return GUID_NAMES.getOrDefault(guid, guid);
    }

    /** HandleProtocol / OpenProtocol: write the interface if the handle has the protocol. */
    private long openProtocol(X86Core c, String service, long handle, long guidPtr, long ifacePtr)
    {
        String guid = guid(c, guidPtr);
        Map<String, Long> protocols = handles.get(handle);
        if (protocols == null) {
            message(String.format("[UEFI] %s(%s): 0x%X is not a handle", service, guidName(guid), handle));
            return EFI_INVALID_PARAMETER;
        }
        Long iface = protocols.get(guid);
        if (iface == null) {
            message(String.format("[UEFI] %s(%s) on handle 0x%X: not supported", service, guidName(guid), handle));
            return EFI_UNSUPPORTED;
        }
        if (ifacePtr != 0) {
            c.writeMem64(ifacePtr, iface);
        }
        return EFI_SUCCESS;
    }

    private long locateProtocol(X86Core c, long guidPtr, long ifacePtr)
    {
        String guid = guid(c, guidPtr);
        for (Map<String, Long> protocols : handles.values()) {
            Long iface = protocols.get(guid);
            if (iface != null) {
                c.writeMem64(ifacePtr, iface);
                return EFI_SUCCESS;
            }
        }
        message("[UEFI] LocateProtocol(" + guidName(guid) + "): not found");
        return EFI_NOT_FOUND;
    }

    /* ---------------- memory ---------------- */

    private long allocatePages(X86Core c, int type, int memoryType, long pages, long memoryPtr)
    {
        long p;
        if (type == 2) {                        // AllocateAddress
            p = c.readMem64(memoryPtr);
            if ((p & 0xFFF) != 0 || !memoryMap.isFree(p, pages)) {
                return EFI_NOT_FOUND;
            }
            memoryMap.set(p, p + pages * X86UefiMemoryMap.PAGE, memoryType);
        } else {                                // AllocateAnyPages, AllocateMaxAddress
            long max = type == 1 ? c.readMem64(memoryPtr) : MEMORY_END - 1;
            p = memoryMap.allocate(pages, memoryType, X86UefiMemoryMap.PAGE, max);
            if (p < 0) {
                return EFI_OUT_OF_RESOURCES;
            }
        }
        c.writeMem64(memoryPtr, p);
        return EFI_SUCCESS;
    }

    private long allocatePool(X86Core c, int memoryType, long size, long bufferPtr)
    {
        long pages = Math.max(1, (size + X86UefiMemoryMap.PAGE - 1) / X86UefiMemoryMap.PAGE);
        long p = memoryMap.allocate(pages, memoryType, X86UefiMemoryMap.PAGE, MEMORY_END - 1);
        if (p < 0) {
            return EFI_OUT_OF_RESOURCES;
        }
        pools.put(p, pages);
        c.writeMem64(bufferPtr, p);
        return EFI_SUCCESS;
    }

    private long freePool(long buffer)
    {
        Long pages = pools.remove(buffer);
        if (pages == null) {
            return EFI_INVALID_PARAMETER;
        }
        memoryMap.set(buffer, buffer + pages * X86UefiMemoryMap.PAGE, X86UefiMemoryMap.CONVENTIONAL);
        return EFI_SUCCESS;
    }

    static final int DESCRIPTOR_SIZE = 48;

    /** GetMemoryMap(*MemoryMapSize, *MemoryMap, *MapKey, *DescriptorSize, *DescriptorVersion). */
    private long getMemoryMap(X86Core c, long[] a)
    {
        List<X86UefiMemoryMap.Range> ranges = memoryMap.getRanges();
        long need = (long) ranges.size() * DESCRIPTOR_SIZE;
        long have = c.readMem64(a[0]);
        c.writeMem64(a[0], need);
        if (a[3] != 0) {
            c.writeMem64(a[3], DESCRIPTOR_SIZE);
        }
        if (a[4] != 0) {
            c.writeMem32(a[4], 1);
        }
        if (have < need) {
            return EFI_BUFFER_TOO_SMALL;
        }
        long d = a[1];
        for (X86UefiMemoryMap.Range r : ranges) {
            boolean runtime = r.type == X86UefiMemoryMap.RUNTIME_SERVICES_CODE
                    || r.type == X86UefiMemoryMap.RUNTIME_SERVICES_DATA;
            c.writeMem32(d, r.type);
            c.writeMem32(d + 4, 0);
            c.writeMem64(d + 8, r.start);                               // PhysicalStart
            c.writeMem64(d + 16, 0);                                    // VirtualStart
            c.writeMem64(d + 24, (r.end - r.start) / X86UefiMemoryMap.PAGE);
            c.writeMem64(d + 32, r.type == X86UefiMemoryMap.RESERVED ? 0
                    : 0xFL | (runtime ? 0x8000000000000000L : 0));     // UC|WC|WT|WB, RUNTIME
            for (int i = 40; i < DESCRIPTOR_SIZE; i++) {
                c.writeMem8(d + i, 0);
            }
            d += DESCRIPTOR_SIZE;
        }
        if (a[2] != 0) {
            c.writeMem64(a[2], memoryMap.getKey());
        }
        return EFI_SUCCESS;
    }

    /* ---------------- calls ---------------- */

    private void trap(X86Core c, long address) throws Exception
    {
        int index = (int) ((address - TRAPS) / 8);
        if ((address - TRAPS) % 8 != 0 || index >= services.size()) {
            throw new IllegalStateException(String.format(
                    "jump into the firmware trap range at 0x%X, which is not a service", address));
        }
        Entry e = services.get(index);
        if (index == 0) {
            e.service.call(c, null);
            return;
        }
        long rsp = c.getReg64(X86Core.REG_RSP);
        long[] args = {
            c.getReg64(X86Core.REG_RCX), c.getReg64(X86Core.REG_RDX),
            c.getReg64(X86Core.REG_R8), c.getReg64(X86Core.REG_R9),
            c.readMem64(rsp + 40), c.readMem64(rsp + 48),
            c.readMem64(rsp + 56), c.readMem64(rsp + 64)
        };
        Service s = e.service;
        long status;
        if (s == null) {
            message("[UEFI] " + e.name + ": not implemented");
            status = EFI_UNSUPPORTED;
        } else {
            status = s.call(c, args);
        }
        if (exited) {
            return;
        }
        c.setReg64(X86Core.REG_RAX, status);
        c.setPC64(c.pop64());
    }

    private long exit(long status)
    {
        exited = true;
        exitStatus = status;
        message(String.format("EFI application exited with status 0x%X.", status));
        core.stopSimulation();
        return status;
    }

    /* ---------------- services ---------------- */

    private Service implementation(String name)
    {
        switch (name) {
            case "ConOut.OutputString":
                return (c, a) -> {
                    StringBuilder sb = new StringBuilder();
                    for (long p = a[1]; ; p += 2) {
                        int ch = c.readMem16(p);
                        if (ch == 0) {
                            break;
                        }
                        sb.append((char) ch);
                    }
                    print(sb.toString());
                    return EFI_SUCCESS;
                };
            case "ConOut.Reset":
            case "ConOut.TestString":
            case "ConOut.SetAttribute":
            case "ConOut.ClearScreen":
            case "ConOut.SetCursorPosition":
            case "ConOut.EnableCursor":
            case "ConIn.Reset":
            case "BootServices.SetWatchdogTimer":
            case "BootServices.CloseProtocol":
                return (c, a) -> EFI_SUCCESS;
            case "BootServices.Stall":
                return (c, a) -> {
                    c.addTimestampCounter(a[0] * TSC_PER_MICROSECOND);
                    return EFI_SUCCESS;
                };
            case "ConOut.QueryMode":
                return (c, a) -> {
                    if (a[1] != 0) {
                        return EFI_UNSUPPORTED;
                    }
                    c.writeMem64(a[2], 80);
                    c.writeMem64(a[3], 25);
                    return EFI_SUCCESS;
                };
            case "ConOut.SetMode":
                return (c, a) -> a[1] == 0 ? EFI_SUCCESS : EFI_UNSUPPORTED;
            case "ConIn.ReadKeyStroke":
                return (c, a) -> {
                    int ch = keyboard.getAsInt();
                    if (ch < 0) {
                        return EFI_NOT_READY;
                    }
                    if (ch == '\n') {
                        ch = '\r';                 // Enter
                    } else if (ch == 0x7F) {
                        ch = 0x08;                  // Backspace
                    }
                    c.writeMem16(a[1], 0);          // EFI_INPUT_KEY.ScanCode
                    c.writeMem16(a[1] + 2, ch);     // UnicodeChar
                    return EFI_SUCCESS;
                };
            case "BootServices.RaiseTPL":
            case "BootServices.RestoreTPL":
                return (c, a) -> 4;             // TPL_APPLICATION
            case "BootServices.AllocatePool":
                return (c, a) -> allocatePool(c, (int) a[0], a[1], a[2]);
            case "BootServices.FreePool":
                return (c, a) -> freePool(a[0]);
            case "BootServices.AllocatePages":
                return (c, a) -> allocatePages(c, (int) a[0], (int) a[1], a[2], a[3]);
            case "BootServices.FreePages":
                return (c, a) -> {
                    memoryMap.set(a[0], a[0] + a[1] * X86UefiMemoryMap.PAGE, X86UefiMemoryMap.CONVENTIONAL);
                    return EFI_SUCCESS;
                };
            case "BootServices.GetMemoryMap":
                return this::getMemoryMap;
            case "BootServices.HandleProtocol":
                return (c, a) -> openProtocol(c, "HandleProtocol", a[0], a[1], a[2]);
            case "BootServices.OpenProtocol":
                return (c, a) -> openProtocol(c, "OpenProtocol", a[0], a[1], a[2]);
            case "BlockIo.ReadBlocks":
                return this::readBlocks;
            case "BlockIo.WriteBlocks":
                return (c, a) -> EFI_WRITE_PROTECTED;
            case "BlockIo.Reset":
            case "BlockIo.FlushBlocks":
                return (c, a) -> EFI_SUCCESS;
            case "BootServices.LocateHandle":
                return this::locateHandle;
            case "BootServices.LocateHandleBuffer":
                return this::locateHandleBuffer;
            case "BootServices.LocateProtocol":
                return (c, a) -> locateProtocol(c, a[0], a[2]);
            case "BootServices.CopyMem":
                return (c, a) -> {
                    byte[] tmp = new byte[(int) a[2]];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = (byte) c.readMem8(a[1] + i);
                    }
                    for (int i = 0; i < tmp.length; i++) {
                        c.writeMem8(a[0] + i, tmp[i]);
                    }
                    return EFI_SUCCESS;
                };
            case "BootServices.SetMem":
                return (c, a) -> {
                    for (long i = 0; i < a[1]; i++) {
                        c.writeMem8(a[0] + i, (int) a[2]);
                    }
                    return EFI_SUCCESS;
                };
            case "BootServices.Exit":
                return (c, a) -> exit(a[1]);
            default:
                return null;
        }
    }

    private void print(String s)
    {
        if (!s.isEmpty()) {
            console.accept(s);
            atLineStart = s.endsWith("\n");
        }
    }

    /** A firmware message on a line of its own. */
    private void message(String s)
    {
        print((atLineStart ? "" : "\n") + s + "\n");
    }

    private void writeUcs2(long addr, String s)
    {
        for (int i = 0; i < s.length(); i++) {
            core.writeMem16(addr + 2L * i, s.charAt(i));
        }
        core.writeMem16(addr + 2L * s.length(), 0);
    }
}

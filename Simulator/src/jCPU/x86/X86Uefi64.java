package jCPU.x86;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
 *   0x00100000  tables, strings, handles
 *   0x00101000  trap range, 8 bytes per service
 *   0x00400000  stack (grows down from 0x00800000)
 *   0x01000000  the application, unless its own ImageBase fits
 *   0x02000000  pool and page allocations, up to the end of memory
 * </pre>
 */
public class X86Uefi64
{
    static final long EFI_SUCCESS = 0;
    static final long EFI_INVALID_PARAMETER = 0x8000000000000002L;
    static final long EFI_UNSUPPORTED = 0x8000000000000003L;
    static final long EFI_NOT_READY = 0x8000000000000006L;
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
    static final long TRAPS = FIRMWARE + 0x1000;
    static final int MAX_SERVICES = 256;
    static final long STACK_TOP = 0x00800000L;
    static final long IMAGE_LOAD = 0x01000000L;
    static final long HEAP = 0x02000000L;
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
    private final List<Entry> services = new ArrayList<>();
    private long heapNext = HEAP;
    private boolean exited;
    private boolean atLineStart = true;
    private long exitStatus;

    public X86Uefi64(X86Core core, Consumer<String> console)
    {
        this.core = core;
        this.console = console;
    }

    /**
     * Load the application, build the tables and set up the registers for
     * its entry: RCX = ImageHandle, RDX = SystemTable, and a return address
     * on the stack that ends the run.
     *
     * @param pe
     * @param peImage
     * @return the entry point, or 0 if the image cannot be placed in memory
     */
    public long load(X86PeLoader pe, byte[] peImage)
    {
        long base = IMAGE_LOAD;
        if (pe.imageBase >= IMAGE_LOAD && pe.imageBase + pe.sizeOfImage <= HEAP) {
            base = pe.imageBase;
        }
        if (base + pe.sizeOfImage > HEAP) {
            message("ERROR: image of " + pe.sizeOfImage + " bytes does not fit below 0x"
                    + Long.toHexString(HEAP) + ".");
            return 0;
        }
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
            case "BootServices.Stall":
            case "BootServices.FreePool":
            case "BootServices.FreePages":
                return (c, a) -> EFI_SUCCESS;
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
                return (c, a) -> EFI_NOT_READY;
            case "BootServices.RaiseTPL":
            case "BootServices.RestoreTPL":
                return (c, a) -> 4;             // TPL_APPLICATION
            case "BootServices.AllocatePool":
                return (c, a) -> {
                    long p = allocate(a[1], 8);
                    if (p == 0) {
                        return EFI_OUT_OF_RESOURCES;
                    }
                    c.writeMem64(a[2], p);
                    return EFI_SUCCESS;
                };
            case "BootServices.AllocatePages":
                return (c, a) -> allocatePages(c, (int) a[0], a[2], a[3]);
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

    /** Bump allocation from the heap; nothing is ever freed. */
    private long allocate(long size, long align)
    {
        long p = (heapNext + align - 1) & -align;
        if (size < 0 || p + size > MEMORY_END) {
            return 0;
        }
        heapNext = p + size;
        return p;
    }

    private long allocatePages(X86Core c, int type, long pages, long memoryPtr)
    {
        long size = pages * 4096;
        long p;
        if (type == 2) {                        // AllocateAddress
            p = c.readMem64(memoryPtr);
            if (p < HEAP || p + size > MEMORY_END || (p & 0xFFF) != 0) {
                return EFI_NOT_FOUND;
            }
            heapNext = Math.max(heapNext, p + size);
        } else {                                // AllocateAnyPages, AllocateMaxAddress
            p = allocate(size, 4096);
            if (p == 0 || (type == 1 && p + size - 1 > c.readMem64(memoryPtr))) {
                return EFI_OUT_OF_RESOURCES;
            }
        }
        c.writeMem64(memoryPtr, p);
        return EFI_SUCCESS;
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

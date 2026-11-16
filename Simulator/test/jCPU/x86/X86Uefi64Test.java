package jCPU.x86;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Runs a few hand-assembled x64 EFI applications against the firmware tables.
 */
public class X86Uefi64Test
{
    private static final long BASE = 0x140000000L;

    private static void put(byte[] b, int off, long v, int size)
    {
        for (int i = 0; i < size; i++) {
            b[off + i] = (byte) (v >>> (8 * i));
        }
    }

    /** PE32+ image with one section at RVA 0x1000 holding {@code text}; entry at its start. */
    private static byte[] image(int... text)
    {
        byte[] f = new byte[0x400];
        f[0] = 'M';
        f[1] = 'Z';
        put(f, 0x3C, 0x40, 4);
        f[0x40] = 'P';
        f[0x41] = 'E';
        int coff = 0x44;
        put(f, coff, 0x8664, 2);
        put(f, coff + 2, 1, 2);
        put(f, coff + 16, 240, 2);
        put(f, coff + 18, 0x0022, 2);       // executable, large address aware
        int opt = coff + 20;
        put(f, opt, 0x20B, 2);
        put(f, opt + 16, 0x1000, 4);
        put(f, opt + 24, BASE, 8);
        put(f, opt + 32, 0x1000, 4);
        put(f, opt + 36, 0x200, 4);
        put(f, opt + 56, 0x2000, 4);
        put(f, opt + 60, 0x200, 4);
        put(f, opt + 108, 16, 4);
        int sec = opt + 240;
        System.arraycopy(".text".getBytes(), 0, f, sec, 5);
        put(f, sec + 8, 0x200, 4);
        put(f, sec + 12, 0x1000, 4);
        put(f, sec + 16, 0x200, 4);
        put(f, sec + 20, 0x200, 4);
        for (int i = 0; i < text.length; i++) {
            f[0x200 + i] = (byte) text[i];
        }
        return f;
    }

    private static final class Run
    {
        final X86Core core = new X86Core(64 * 1024 * 1024);
        final StringBuilder out = new StringBuilder();
        final java.util.ArrayDeque<Integer> keys = new java.util.ArrayDeque<>();
        final X86Uefi64 uefi = new X86Uefi64(core, out::append,
                () -> keys.isEmpty() ? -1 : keys.poll());
        final long entry;

        Run(byte[] image) throws Exception
        {
            this(image, null);
        }

        Run(byte[] image, X86Uefi64.BootDisk disk) throws Exception
        {
            X86PeLoader pe = new X86PeLoader(image);
            if (disk != null) {
                uefi.attachBootDisk(disk);
            }
            entry = uefi.load(pe, image);
            core.setLongMode(true);
            core.setActiveExecutor(new X86Amd64());
            core.setPC64(entry);
        }

        void run() throws Exception
        {
            for (int i = 0; i < 1000 && !uefi.hasExited(); i++) {
                core.step();
            }
            assertTrue("application did not exit", uefi.hasExited());
        }
    }

    @Test
    public void testEntryGetsImageHandleSystemTableAndAlignedStack() throws Exception
    {
        Run r = new Run(image(0xC3));

        assertEquals(X86Uefi64.IMAGE_LOAD + 0x1000, r.entry);
        assertEquals(X86Uefi64.IMAGE_HANDLE, r.core.getReg64(X86Core.REG_RCX));
        assertEquals(X86Uefi64.SYSTEM_TABLE, r.core.getReg64(X86Core.REG_RDX));
        assertEquals(8, r.core.getReg64(X86Core.REG_RSP) % 16);
        assertEquals("IBI SYST", ascii(r.core, X86Uefi64.SYSTEM_TABLE, 8));
    }

    @Test
    public void testReturnFromEntryPointEndsTheRun() throws Exception
    {
        // mov eax, 7 ; ret
        Run r = new Run(image(0xB8, 0x07, 0x00, 0x00, 0x00, 0xC3));
        r.run();

        assertEquals(7, r.uefi.getExitStatus());
    }

    @Test
    public void testOutputStringPrintsThroughConOut() throws Exception
    {
        Run r = new Run(image(
                0x48, 0x83, 0xEC, 0x28,                     // sub rsp, 0x28
                0x48, 0x8B, 0x4A, 0x40,                     // mov rcx, [rdx+64]   ; ConOut
                0x48, 0x8D, 0x15, 0x11, 0x00, 0x00, 0x00,   // lea rdx, [rip+0x11] ; "Hi"
                0xFF, 0x51, 0x08,                           // call [rcx+8]        ; OutputString
                0x48, 0x83, 0xC4, 0x28,                     // add rsp, 0x28
                0xC3,                                       // ret, with OutputString's status in RAX
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                'H', 0, 'i', 0, 0, 0));
        r.run();

        assertTrue(r.out.toString(), r.out.toString().contains("\nHi\nEFI application exited"));
        assertEquals(X86Uefi64.EFI_SUCCESS, r.uefi.getExitStatus());
    }

    @Test
    public void testUnimplementedServiceReturnsUnsupported() throws Exception
    {
        Run r = new Run(image(
                0x48, 0x83, 0xEC, 0x28,                     // sub rsp, 0x28
                0x48, 0x8B, 0x42, 0x60,                     // mov rax, [rdx+96]   ; BootServices
                0xFF, 0x50, 0x50,                           // call [rax+80]       ; CreateEvent
                0x48, 0x83, 0xC4, 0x28,                     // add rsp, 0x28
                0xC3));
        r.run();

        assertEquals(X86Uefi64.EFI_UNSUPPORTED, r.uefi.getExitStatus());
        assertTrue(r.out.toString(), r.out.toString().contains("BootServices.CreateEvent: not implemented"));
    }

    @Test
    public void testAllocatePoolReturnsUsableMemory() throws Exception
    {
        Run r = new Run(image(
                0x48, 0x83, 0xEC, 0x38,                     // sub rsp, 0x38
                0x48, 0x8B, 0x42, 0x60,                     // mov rax, [rdx+96]   ; BootServices
                0xB9, 0x04, 0x00, 0x00, 0x00,               // mov ecx, 4          ; EfiBootServicesData
                0xBA, 0x00, 0x01, 0x00, 0x00,               // mov edx, 0x100
                0x4C, 0x8D, 0x44, 0x24, 0x28,               // lea r8, [rsp+0x28]
                0xFF, 0x50, 0x40,                           // call [rax+64]       ; AllocatePool
                0x48, 0x8B, 0x44, 0x24, 0x28,               // mov rax, [rsp+0x28] ; the buffer
                0x48, 0x83, 0xC4, 0x38,                     // add rsp, 0x38
                0xC3));                                     // exit status = buffer address
        r.run();

        assertEquals(r.core.getMemSize() - 4096, r.uefi.getExitStatus());
    }

    @Test
    public void testLoadedImageProtocolOnImageHandle() throws Exception
    {
        // OpenProtocol(ImageHandle, &LoadedImageGuid, &iface, ImageHandle, 0, BY_HANDLE_PROTOCOL)
        // and return iface->ImageBase.
        Run r = new Run(image(
                0x48, 0x83, 0xEC, 0x48,                     // sub rsp, 0x48
                0x48, 0x8B, 0x42, 0x60,                     // mov rax, [rdx+96]   ; BootServices
                0x48, 0x8D, 0x15, 0x31, 0x00, 0x00, 0x00,   // lea rdx, [rip+0x31] ; GUID at 0x40
                0x4C, 0x8D, 0x44, 0x24, 0x38,               // lea r8, [rsp+0x38]
                0x49, 0x89, 0xC9,                           // mov r9, rcx
                0x48, 0xC7, 0x44, 0x24, 0x20, 0, 0, 0, 0,   // mov qword [rsp+0x20], 0
                0x48, 0xC7, 0x44, 0x24, 0x28, 1, 0, 0, 0,   // mov qword [rsp+0x28], 1
                0xFF, 0x90, 0x18, 0x01, 0x00, 0x00,         // call [rax+280]      ; OpenProtocol
                0x48, 0x8B, 0x44, 0x24, 0x38,               // mov rax, [rsp+0x38]
                0x48, 0x8B, 0x40, 0x40,                     // mov rax, [rax+64]   ; ImageBase
                0x48, 0x83, 0xC4, 0x48,                     // add rsp, 0x48
                0xC3,
                0, 0, 0,
                0xA1, 0x31, 0x1B, 0x5B, 0x62, 0x95, 0xD2, 0x11,
                0x8E, 0x3F, 0x00, 0xA0, 0xC9, 0x69, 0x72, 0x3B));
        r.run();

        assertEquals(X86Uefi64.IMAGE_LOAD, r.uefi.getExitStatus());
    }

    @Test
    public void testReadKeyStrokeGivesTypedCharacters() throws Exception
    {
        // ConIn->ReadKeyStroke(ConIn, &key) and return key (ScanCode | UnicodeChar << 16).
        int[] code = {
                0x48, 0x83, 0xEC, 0x38,                     // sub rsp, 0x38
                0x48, 0x8B, 0x4A, 0x30,                     // mov rcx, [rdx+48]   ; ConIn
                0x48, 0x8D, 0x54, 0x24, 0x28,               // lea rdx, [rsp+0x28]
                0xFF, 0x51, 0x08,                           // call [rcx+8]        ; ReadKeyStroke
                0x48, 0x85, 0xC0,                           // test rax, rax
                0x75, 0x04,                                 // jnz +4              ; keep the error
                0x8B, 0x44, 0x24, 0x28,                     // mov eax, [rsp+0x28]
                0x48, 0x83, 0xC4, 0x38,                     // add rsp, 0x38
                0xC3 };
        Run none = new Run(image(code));
        none.run();
        assertEquals(X86Uefi64.EFI_NOT_READY, none.uefi.getExitStatus());

        Run typed = new Run(image(code));
        typed.keys.add((int) '\n');
        typed.run();
        assertEquals(0x000D0000L, typed.uefi.getExitStatus());
    }

    /** 4096-sector disk whose every byte is the low byte of its sector number; ESP at 100. */
    private static X86Uefi64.BootDisk disk()
    {
        X86Gpt.Partition esp = new X86Gpt.Partition(X86Gpt.EFI_SYSTEM_PARTITION_GUID,
                "01234567-89AB-CDEF-0123-456789ABCDEF", 100, 1099, 0, "ESP", 1);
        return X86Uefi64.BootDisk.gpt(lba -> {
            byte[] b = new byte[512];
            java.util.Arrays.fill(b, (byte) lba);
            return b;
        }, 4096, esp, "EFI/BOOT/BOOTX64.EFI");
    }

    @Test
    public void testLoadedImageComesFromTheBootPartition() throws Exception
    {
        Run r = new Run(image(0xC3), disk());

        assertEquals(X86Uefi64.PARTITION_HANDLE, r.core.readMem64(X86Uefi64.LOADED_IMAGE + 24));
        long path = r.core.readMem64(X86Uefi64.LOADED_IMAGE + 32);
        assertEquals(4, r.core.readMem8(path));                 // media
        assertEquals(4, r.core.readMem8(path + 1));             // file path
        StringBuilder name = new StringBuilder();
        for (long p = path + 4; r.core.readMem16(p) != 0; p += 2) {
            name.append((char) r.core.readMem16(p));
        }
        assertEquals("\\EFI\\BOOT\\BOOTX64.EFI", name.toString());
    }

    @Test
    public void testReadBlocksOnPartitionReadsFromPartitionStart() throws Exception
    {
        // HandleProtocol(PartitionHandle, &BlockIoGuid, &bio), then
        // bio->ReadBlocks(bio, 0, 1, 512, 0x900000) and return its status.
        Run r = new Run(image(
                0x48, 0x83, 0xEC, 0x38,                     // sub rsp, 0x38
                0x48, 0x8B, 0x42, 0x60,                     // mov rax, [rdx+96]   ; BootServices
                0x48, 0xC7, 0xC1, 0x40, 0x07, 0x10, 0x00,   // mov rcx, PARTITION_HANDLE
                0x48, 0x8D, 0x15, 0x3A, 0x00, 0x00, 0x00,   // lea rdx, [rip+0x3A] ; GUID at 80
                0x4C, 0x8D, 0x44, 0x24, 0x28,               // lea r8, [rsp+0x28]
                0xFF, 0x90, 0x98, 0x00, 0x00, 0x00,         // call [rax+152]      ; HandleProtocol
                0x48, 0x85, 0xC0,                           // test rax, rax
                0x75, 0x1F,                                 // jnz end
                0x48, 0x8B, 0x4C, 0x24, 0x28,               // mov rcx, [rsp+0x28] ; BlockIo
                0x31, 0xD2,                                 // xor edx, edx        ; MediaId
                0x41, 0xB8, 0x01, 0x00, 0x00, 0x00,         // mov r8d, 1          ; LBA
                0x41, 0xB9, 0x00, 0x02, 0x00, 0x00,         // mov r9d, 512
                0x48, 0xC7, 0x44, 0x24, 0x20, 0x00, 0x00, 0x90, 0x00, // mov qword [rsp+0x20], 0x900000
                0xFF, 0x51, 0x18,                           // call [rcx+24]       ; ReadBlocks
                0x48, 0x83, 0xC4, 0x38,                     // end: add rsp, 0x38
                0xC3,
                0, 0, 0, 0, 0, 0,
                0x21, 0x5B, 0x4E, 0x96, 0x59, 0x64, 0xD2, 0x11,
                0x8E, 0x39, 0x00, 0xA0, 0xC9, 0x69, 0x72, 0x3B), disk());
        r.run();

        assertEquals(X86Uefi64.EFI_SUCCESS, r.uefi.getExitStatus());
        assertEquals(101, r.core.readMem8(0x900000));
        assertEquals(101, r.core.readMem8(0x9001FF));
        assertEquals(0, r.core.readMem8(0x900200));
    }

    @Test
    public void testGuidBytesUseMixedEndianLayout()
    {
        byte[] b = X86Uefi64.guidBytes("C12A7328-F81F-11D2-BA4B-00A0C93EC93B");
        int[] expected = { 0x28, 0x73, 0x2A, 0xC1, 0x1F, 0xF8, 0xD2, 0x11,
                           0xBA, 0x4B, 0x00, 0xA0, 0xC9, 0x3E, 0xC9, 0x3B };
        for (int i = 0; i < 16; i++) {
            assertEquals("byte " + i, expected[i], b[i] & 0xFF);
        }
    }

    private static String ascii(X86Core core, long addr, int n)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) core.readMem8(addr + i));
        }
        return sb.toString();
    }
}

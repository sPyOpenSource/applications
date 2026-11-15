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
        final X86Uefi64 uefi = new X86Uefi64(core, out::append);
        final long entry;

        Run(byte[] image) throws Exception
        {
            X86PeLoader pe = new X86PeLoader(image);
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
                0xFF, 0x50, 0x38,                           // call [rax+56]       ; GetMemoryMap
                0x48, 0x83, 0xC4, 0x28,                     // add rsp, 0x28
                0xC3));
        r.run();

        assertEquals(X86Uefi64.EFI_UNSUPPORTED, r.uefi.getExitStatus());
        assertTrue(r.out.toString(), r.out.toString().contains("BootServices.GetMemoryMap: not implemented"));
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

        assertEquals(X86Uefi64.HEAP, r.uefi.getExitStatus());
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

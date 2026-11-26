package jCPU.x86;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * System instructions of the x86-64 executor: descriptor tables, segment
 * registers, control registers, EFER and far branches, and the switch from
 * long mode to 32-bit protected mode they make possible.
 */
public class X86SystemTest
{
    private static final int GDT = 0x1000;
    private static final int CODE = 0x10000;
    private static final int IDT = 0x2000;
    private static final int HANDLER = 0x10100;

    /** Flat 32-bit code (0x08) and data (0x10) segments, and data with base 0x5000 (0x18). */
    private static final long[] FLAT_GDT = {
        0,
        0x00CF9A000000FFFFL,
        0x00CF92000000FFFFL,
        0x00CF92005000FFFFL,
    };

    private static X86Core core32(int... code)
    {
        X86Core core = new X86Core(1024 * 1024);
        core.enterProtectedMode32();
        core.setActiveExecutor(new X86Amd64());
        for (int i = 0; i < FLAT_GDT.length; i++) {
            core.writeMem64(GDT + 8L * i, FLAT_GDT[i]);
        }
        core.setGdt(GDT, 8 * FLAT_GDT.length - 1);
        core.setSegSelector(X86Core.SEG_CS, 0x08);
        load(core, CODE, code);
        core.setPC64(CODE);
        core.setReg64(X86Core.REG_RSP, 0x8000);
        return core;
    }

    private static void load(X86Core core, int address, int... code)
    {
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(address + i, code[i]);
        }
    }

    /** A flat 32-bit interrupt gate for vector 1, and an IRETD-only handler. */
    private static void installDebugGate(X86Core core)
    {
        core.setIdt(IDT, 0x10);
        long gate = (HANDLER & 0xFFFFL)
                | (0x08L << 16)                         // flat 32-bit code selector
                | (0x8EL << 40)                         // present, DPL 0, 32-bit interrupt gate
                | (((HANDLER >>> 16) & 0xFFFFL) << 48);
        core.writeMem64(IDT + 8, gate);                 // vector 1 (#DB)
        load(core, HANDLER, 0xCF);                      // iretd
    }

    private static void steps(X86Core core, int n) throws Exception
    {
        for (int i = 0; i < n; i++) {
            core.step();
        }
    }

    private static String unknown(X86Core core, int steps) throws Exception
    {
        try {
            steps(core, steps);
        } catch (X86UnknownOpcodeException ex) {
            return ex.getMessage();
        }
        fail("expected X86UnknownOpcodeException");
        return null;
    }

    /**
     * What GRUB 2.00's relocator does to start a 32-bit Multiboot kernel from
     * x64 UEFI (PREAMBLE and RELOAD_GDT in relocator_common.S, then
     * relocator32.S): load its own GDT, far jump to a 32-bit code segment,
     * which is compatibility mode, load the data segments, turn off paging,
     * EFER.LME and PAE, and jump to the kernel.  Assembled with GNU as.
     */
    private static final int[] GRUB_LEAVE_LONG_MODE = {
            0x48, 0x89, 0xC6,                            // 00: mov rsi,rax
            0x48, 0x05, 0x0B, 0x00, 0x00, 0x00,          // 03: add rax,0xb
            0xFF, 0xE0,                                  // 09: jmp rax
            0x48, 0x8D, 0x86, 0x33, 0x00, 0x00, 0x00,    // 0b: lea rax,[rsi+0x33]
            0x89, 0x86, 0xAA, 0x00, 0x00, 0x00,          // 12: mov DWORD PTR [rsi+0xaa],eax
            0x48, 0x8D, 0x86, 0x80, 0x00, 0x00, 0x00,    // 18: lea rax,[rsi+0x80]
            0x48, 0x89, 0x86, 0xA2, 0x00, 0x00, 0x00,    // 1f: mov QWORD PTR [rsi+0xa2],rax
            0x0F, 0x01, 0x96, 0xA0, 0x00, 0x00, 0x00,    // 26: lgdt [rsi+0xa0]
            0xFF, 0xAE, 0xAA, 0x00, 0x00, 0x00,          // 2d: jmp FWORD PTR [rsi+0xaa]
            0xB8, 0x18, 0x00, 0x00, 0x00,                // 33: mov eax,0x18
            0x8E, 0xD8,                                  // 38: mov ds,eax
            0x8E, 0xC0,                                  // 3a: mov es,eax
            0x8E, 0xE0,                                  // 3c: mov fs,eax
            0x8E, 0xE8,                                  // 3e: mov gs,eax
            0x8E, 0xD0,                                  // 40: mov ss,eax
            0x0F, 0x20, 0xC0,                            // 42: mov eax,cr0
            0x25, 0xFF, 0xFF, 0xFF, 0x7F,                // 45: and eax,0x7fffffff
            0x0F, 0x22, 0xC0,                            // 4a: mov cr0,eax
            0xB9, 0x80, 0x00, 0x00, 0xC0,                // 4d: mov ecx,0xc0000080
            0x0F, 0x32,                                  // 52: rdmsr
            0x25, 0xFF, 0xFE, 0xFF, 0xFF,                // 54: and eax,0xfffffeff
            0x0F, 0x30,                                  // 59: wrmsr
            0x0F, 0x20, 0xE0,                            // 5b: mov eax,cr4
            0x83, 0xE0, 0xDF,                            // 5e: and eax,0xffffffdf
            0x0F, 0x22, 0xE0,                            // 61: mov cr4,eax
            0xEB, 0x00,                                  // 64: jmp 0x66
            0xB8, 0x00, 0x70, 0x00, 0x00,                // 66: mov eax,0x7000
            0x89, 0xC4,                                  // 6b: mov esp,eax
            0xB8, 0x02, 0xB0, 0xAD, 0x2B,                // 6d: mov eax,0x2badb002
            0xBB, 0x00, 0x90, 0x00, 0x00,                // 72: mov ebx,0x9000
            0xFC,                                        // 77: cld
            0xEA, 0x00, 0x00, 0x02, 0x00, 0x10, 0x00,    // 78: jmp 0x10:0x20000
            0x90,                                        // 7f: padding
            // 80: GDT: null, reserved, flat 32-bit code (0x10), flat data (0x18)
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0xFF, 0xFF, 0x00, 0x00, 0x00, 0x9A, 0xCF, 0x00,
            0xFF, 0xFF, 0x00, 0x00, 0x00, 0x92, 0xCF, 0x00,
            0x1F, 0x00,                                  // a0: GDT limit
            0, 0, 0, 0, 0, 0, 0, 0,                      // a2: GDT base, filled in
            0, 0, 0, 0, 0x10, 0x00,                      // aa: far pointer to cont1, filled in
    };

    @Test
    public void testLeaveLongModeLikeGrub() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.setLongMode(true);
        core.setActiveExecutor(new X86Amd64());
        load(core, CODE, GRUB_LEAVE_LONG_MODE);
        load(core, 0x20000, 0x50, 0x5A);                // the kernel: push eax ; pop edx
        core.setReg64(X86Core.REG_RAX, CODE);           // GRUB passes the base in RAX
        core.setReg64(X86Core.REG_RSP, 0x8000);
        core.setPC64(CODE);

        steps(core, 9);                                 // up to and including the far jump
        assertEquals(CODE + 0x33, core.getPC64());
        assertTrue("compatibility mode", core.isLongMode() && !core.isCodeLong());
        assertTrue(core.isCodeSize32());
        assertEquals(0x10, core.getSegSelector(X86Core.SEG_CS));
        assertEquals(CODE + 0x80, core.getGdtBase64());
        assertEquals(0x1F, core.getGdtLimit());

        for (int i = 0; i < 100 && core.getPC64() != 0x20000; i++) {
            core.step();
        }
        assertEquals(0x20000, core.getPC64());
        assertFalse("long mode is off", core.isLongMode());
        assertEquals(0, core.getEfer() & (X86Core.EFER_LME | X86Core.EFER_LMA));
        assertEquals(0, core.getCr0() & X86Core.CR0_PG);
        assertEquals(X86Core.CR0_PE, core.getCr0() & X86Core.CR0_PE);
        assertEquals(0, core.getCr4() & X86Core.CR4_PAE);
        assertEquals(0x10, core.getSegSelector(X86Core.SEG_CS));
        assertTrue(core.isCodeSize32());
        assertFalse(core.isCodeLong());
        for (int seg : new int[] { X86Core.SEG_DS, X86Core.SEG_ES, X86Core.SEG_FS,
                                   X86Core.SEG_GS, X86Core.SEG_SS }) {
            assertEquals(0x18, core.getSegSelector(seg));
        }
        assertEquals(0x2BADB002L, core.getReg64(X86Core.REG_RAX));
        assertEquals(0x9000, core.getReg64(X86Core.REG_RBX));
        assertEquals(0x7000, core.getReg64(X86Core.REG_RSP));

        steps(core, 2);                                 // 32-bit stack: 4 bytes per push
        assertEquals(0x2BADB002L, core.getReg64(X86Core.REG_RDX));
        assertEquals(0x2BADB002, core.readMem32(0x7000 - 4));
        assertEquals(0x7000, core.getReg64(X86Core.REG_RSP));
    }

    /**
     * A 16-bit writable data segment is a legal SS in 32-bit protected mode:
     * Intel's MOV-to-SS checks require a present writable data segment with
     * matching privilege, but place no requirement on the descriptor's D/B
     * bit.  GRUB's 32-bit relocator loads exactly such a descriptor before
     * leaving long mode.
     */
    @Test
    public void testLoad16BitWritableDataSegmentIntoSS() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.enterProtectedMode32();
        core.setActiveExecutor(new X86Amd64());
        long[] gdt = {
            0,
            0x00CF9A000000FFFFL,                       // 0x08: flat 32-bit code
            0x008F92000000FFFFL,                       // 0x10: flat 16-bit writable data
        };
        for (int i = 0; i < gdt.length; i++) {
            core.writeMem64(GDT + 8L * i, gdt[i]);
        }
        core.setGdt(GDT, 8 * gdt.length - 1);
        core.setSegSelector(X86Core.SEG_CS, 0x08);
        load(core, CODE,
             0x66, 0xB8, 0x10, 0x00,                  // mov ax, 0x10
             0x8E, 0xD0,                              // mov ss, eax
             0xEB, 0xFE);                             // jmp $
        core.setPC64(CODE);
        core.setReg64(X86Core.REG_RSP, 0x8000);

        steps(core, 3);

        assertEquals(0x10, core.getSegSelector(X86Core.SEG_SS));
        assertEquals(0, core.getSegBase(X86Core.SEG_SS));
    }

    /**
     * GRUB's 16-bit relocator far-jumps from 32-bit compatibility mode to a
     * 16-bit code descriptor before clearing CR0.PE.  Execution must continue
     * there: the default operand size is 16 bits, and 0x66 selects 32 bits.
     */
    @Test
    public void testFarJumpTo16BitCodeSegment() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.enterProtectedMode32();
        core.setActiveExecutor(new X86Amd64());
        long[] gdt = {
            0,
            0x00CF9A000000FFFFL,                       // 0x08: flat 32-bit code
            0x00CF92000000FFFFL,                       // 0x10: flat 32-bit data
            0x00009E000000FFFFL,                       // 0x18: 16-bit conforming code
            0x000092000000FFFFL,                       // 0x20: 16-bit data
        };
        for (int i = 0; i < gdt.length; i++) {
            core.writeMem64(GDT + 8L * i, gdt[i]);
        }
        core.setGdt(GDT, 8 * gdt.length - 1);
        core.setSegSelector(X86Core.SEG_CS, 0x08);
        load(core, CODE,
              0xEA, 0x00, 0x80, 0x00, 0x00, 0x18, 0x00); // jmp far 0x18:0x8000
        load(core, 0x8000,
              0x66, 0xB8, 0x01, 0x00, 0x01, 0x00,       // mov eax, 0x00010001
              0xEB, 0xFE);                              // jmp $
        core.setPC64(CODE);

        steps(core, 3);

        assertEquals(0x18, core.getSegSelector(X86Core.SEG_CS));
        assertFalse(core.isCodeSize32());
        assertEquals(0x00010001L, core.getReg64(X86Core.REG_RAX));
        assertEquals(0x8006, core.getPC64());
    }

    /**
     * GRUB's 16-bit relocator clears CR0.PE and reloads CS with a real-mode far
     * jump.  The executor must leave protected mode and keep executing the
     * 16-bit code after the jump.
     */
    @Test
    public void testLeaveProtectedModeForRealMode() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.enterProtectedMode32();
        core.setActiveExecutor(new X86Amd64());
        long[] gdt = {
            0,
            0x00CF9A000000FFFFL,                       // 0x08: flat 32-bit code
            0x00009E000000FFFFL,                       // 0x10: 16-bit conforming code
        };
        for (int i = 0; i < gdt.length; i++) {
            core.writeMem64(GDT + 8L * i, gdt[i]);
        }
        core.setGdt(GDT, 8 * gdt.length - 1);
        core.setSegSelector(X86Core.SEG_CS, 0x08);
        load(core, CODE,
              0xEA, 0x00, 0x80, 0x00, 0x00, 0x10, 0x00); // jmp far 0x10:0x8000
        load(core, 0x8000,
              0x0F, 0x20, 0xC0,                          // mov eax, cr0
              0x66, 0x83, 0xE0, 0xFE,                    // and eax, ~1
              0x0F, 0x22, 0xC0,                          // mov cr0, eax
              0x66, 0xEA, 0x7C, 0x80, 0x00, 0x00, 0x00, 0x00, // jmp far 0:0x807C
              0xEB, 0xFE);                               // padding
        load(core, 0x807C,
              0xB8, 0x34, 0x12,                          // mov ax, 0x1234
              0xEB, 0xFE);                               // jmp $
        core.setPC64(CODE);

        steps(core, 7);

        assertFalse(core.isProtectedMode());
        assertFalse(core.isCodeSize32());
        assertEquals(0, core.getSegSelector(X86Core.SEG_CS));
        assertEquals(0x1234, core.getReg64(X86Core.REG_RAX) & 0xFFFF);
        assertEquals(0x807F, core.getPC64());
    }

    /** In real mode both CS:IP and data segment registers use selector * 16. */
    @Test
    public void testRealModeUsesSegmentBases() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.enterProtectedMode32();
        core.setActiveExecutor(new X86Amd64());
        long[] gdt = {
            0,
            0x00CF9A000000FFFFL,                       // 0x08: flat 32-bit code
            0x00009E000000FFFFL,                       // 0x10: 16-bit conforming code
        };
        for (int i = 0; i < gdt.length; i++) {
            core.writeMem64(GDT + 8L * i, gdt[i]);
        }
        core.setGdt(GDT, 8 * gdt.length - 1);
        core.setSegSelector(X86Core.SEG_CS, 0x08);
        load(core, CODE,
              0xEA, 0x00, 0x80, 0x00, 0x00, 0x10, 0x00); // jmp far 0x10:0x8000
        load(core, 0x8000,
              0x0F, 0x20, 0xC0,                          // mov eax, cr0
              0x66, 0x83, 0xE0, 0xFE,                    // and eax, ~1
              0x0F, 0x22, 0xC0,                          // mov cr0, eax
              0x66, 0xEA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20); // jmp 0x2000:0
        load(core, 0x20000,
              0xB8, 0x00, 0x20,                          // mov ax, 0x2000
              0x8E, 0xD8,                                // mov ds, ax
              0xA1, 0x10, 0x00,                          // mov ax, ds:[0x10]
              0xEB, 0xFE);                               // jmp $
        core.writeMem16(0x20010, 0xABCD);
        core.setPC64(CODE);

        steps(core, 9);

        assertEquals(0x2000, core.getSegSelector(X86Core.SEG_CS));
        assertEquals(0x20000, core.getSegBase(X86Core.SEG_CS));
        assertEquals(0x20000, core.getSegBase(X86Core.SEG_DS));
        assertEquals(0xABCD, core.getReg64(X86Core.REG_RAX) & 0xFFFF);
        assertEquals(0x20008, core.getPC64());
    }

    @Test
    public void testSegmentBaseComesFromTheDescriptor() throws Exception
    {
        X86Core core = core32(
                0x66, 0xB8, 0x18, 0x00,                 // mov ax, 0x18
                0x8E, 0xE0,                             // mov fs, eax
                0x64, 0xA1, 0x10, 0x00, 0x00, 0x00,     // mov eax, fs:[0x10]
                0x8B, 0x1D, 0x10, 0x00, 0x00, 0x00);    // mov ebx, [0x10]
        core.writeMem32(0x5010, 0x12345678);
        core.writeMem32(0x10, 0xAAAA);
        steps(core, 4);
        assertEquals(0x5000, core.getSegBase(X86Core.SEG_FS));
        assertEquals(0x12345678, core.getReg64(X86Core.REG_RAX));
        assertEquals(0xAAAA, core.getReg64(X86Core.REG_RBX));
    }

    @Test
    public void testPushAndPopSegmentRegisters() throws Exception
    {
        X86Core core = core32(
                0x6A, 0x18,                             // push 0x18
                0x07,                                   // pop es
                0x06,                                   // push es
                0x0F, 0xA9,                             // pop gs
                0x8C, 0xE8);                            // mov eax, gs
        steps(core, 5);
        assertEquals(0x18, core.getSegSelector(X86Core.SEG_ES));
        assertEquals(0x5000, core.getSegBase(X86Core.SEG_GS));
        assertEquals(0x18, core.getReg64(X86Core.REG_RAX));
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));
    }

    @Test
    public void testFarCallAndReturnIn32BitCode() throws Exception
    {
        X86Core core = core32(
                0x9A, 0x00, 0x01, 0x01, 0x00, 0x08, 0x00,   // call 0x08:0x10100
                0x90);                                      // nop
        load(core, 0x10100,
                0xBB, 0x05, 0x00, 0x00, 0x00,           // mov ebx, 5
                0xCB);                                  // retf
        core.step();
        assertEquals(0x10100, core.getPC64());
        assertEquals(0x8000 - 8, core.getReg64(X86Core.REG_RSP));
        assertEquals(CODE + 7, core.readMem32(0x8000 - 8));
        assertEquals(0x08, core.readMem32(0x8000 - 4));
        steps(core, 2);
        assertEquals(CODE + 7, core.getPC64());
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));
        assertEquals(5, core.getReg64(X86Core.REG_RBX));
    }

    @Test
    public void testSgdtStoresWhatLgdtLoaded() throws Exception
    {
        X86Core core = new X86Core(1024 * 1024);
        core.setLongMode(true);
        core.setActiveExecutor(new X86Amd64());
        load(core, CODE,
                0x0F, 0x01, 0x15, 0x08, 0x00, 0x00, 0x00,   // lgdt [rip+8]
                0x0F, 0x01, 0x07,                           // sgdt [rdi]
                0x90, 0x90, 0x90, 0x90, 0x90,
                0x37, 0x00,                                 // limit 0x37
                0x00, 0x30, 0x02, 0x00, 0x01, 0x00, 0x00, 0x00);  // base 0x100023000
        core.setReg64(X86Core.REG_RDI, 0x3000);
        core.setPC64(CODE);
        steps(core, 2);
        assertEquals(0x100023000L, core.getGdtBase64());
        assertEquals(0x37, core.getGdtLimit());
        assertEquals(0x37, core.readMem16(0x3000));
        assertEquals(0x100023000L, core.readMem64(0x3002L));
    }

    @Test
    public void testLdtSelectorStops() throws Exception
    {
        X86Core core = core32(
                0x66, 0xB8, 0x0C, 0x00,                 // mov ax, 0x0C (TI = 1)
                0x8E, 0xD8);                            // mov ds, eax
        String message = unknown(core, 2);
        assertTrue(message, message.contains("load DS: selector 0xC is in the LDT"));
        assertEquals(CODE + 4, core.getPC64());
    }

    @Test
    public void testUnknownMsrStops() throws Exception
    {
        X86Core core = core32(
                0xB9, 0x1B, 0x00, 0x00, 0x00,           // mov ecx, 0x1B (APIC base)
                0x0F, 0x32);                            // rdmsr
        String message = unknown(core, 2);
        assertTrue(message, message.contains("RDMSR 0x1B in 32-bit code"));
    }

    @Test
    public void testPopfKeepsReservedBitOne() throws Exception
    {
        // Even POPF 0 must leave EFLAGS bit 1 set; x86 does not allow it to be cleared.
        X86Core core = core32(
                0x6A, 0x00,                             // push 0
                0x9D);                                  // popf
        steps(core, 2);
        assertEquals(2, core.getRflags64() & 2);
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));
    }

    @Test
    public void testDebugRegistersReadBack() throws Exception
    {
        X86Core core = core32(
                0xB8, 0xF0, 0x0F, 0xFF, 0xFF,           // mov eax, 0xFFFF0FF0
                0x0F, 0x23, 0xF0,                       // mov dr6, eax
                0x0F, 0x21, 0xF3,                       // mov ebx, dr6
                0x0F, 0x21, 0xE9);                      // mov ecx, dr5 (DR7)
        steps(core, 4);
        assertEquals(0xFFFF0FF0L, core.getReg64(X86Core.REG_RBX));
        assertEquals(0x400, core.getReg64(X86Core.REG_RCX));
        assertFalse(core.hasBreakpoints());
    }

    @Test
    public void testWriteBreakpointDeliversDebugException() throws Exception
    {
        // What jxcore does to catch writes through a null pointer:
        // breakpoint 1 on writes to address 0..3.
        X86Core core = core32(
                0x31, 0xC0,                             // xor eax, eax
                0x0F, 0x23, 0xC8,                       // mov dr1, eax
                0xB8, 0x08, 0x02, 0xD0, 0x00,           // mov eax, 0xD00208 (G1, GE, write, 4 bytes)
                0x0F, 0x23, 0xF8,                       // mov dr7, eax
                0x89, 0x1D, 0x00, 0x01, 0x00, 0x00,     // mov [0x100], ebx
                0x8B, 0x1D, 0x00, 0x00, 0x00, 0x00,     // mov ebx, [0]: reads do not count
                0xB3, 0x5A,                             // mov bl, 0x5A
                0x88, 0x1D, 0x02, 0x00, 0x00, 0x00);    // mov [2], bl
        installDebugGate(core);
        steps(core, 7);
        assertTrue(core.hasBreakpoints());

        core.step();                                    // #DB: write happens, then the trap is delivered
        assertEquals(HANDLER, core.getPC64());
        assertEquals(0x5A, core.readMem8(2));           // the write happened before the trap
        assertEquals(0x8000 - 12, core.getReg64(X86Core.REG_RSP));
        assertEquals(CODE + 33, core.readMem32(0x8000 - 12));
        assertEquals(0x08, core.readMem32(0x8000 - 8));
        assertEquals(1 << 1, core.getDebugReg(6) & 0xF);

        core.step();                                    // iretd
        assertEquals(CODE + 33, core.getPC64());
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));
    }

    /**
     * jxcore enables a write breakpoint on address 0 very early, before the
     * guest has loaded an IDT.  The write must still happen and execution must
     * continue instead of stopping on an undeliverable #DB.
     */
    @Test
    public void testBreakpointWithoutIdtIsIgnored() throws Exception
    {
        X86Core core = core32(
                0x31, 0xC0,                             // xor eax, eax
                0x0F, 0x23, 0xC8,                       // mov dr1, eax
                0xB8, 0x08, 0x02, 0xD0, 0x00,           // mov eax, 0xD00208 (G1, write, 4 bytes)
                0x0F, 0x23, 0xF8,                       // mov dr7, eax
                0xB8, 0x5A, 0x00, 0x00, 0x00,           // mov eax, 0x5A
                0x89, 0x05, 0x00, 0x00, 0x00, 0x00);    // mov [0], eax
        steps(core, 6);

        assertTrue(core.hasBreakpoints());
        assertEquals(0x5A, core.readMem32(0));
        assertEquals(CODE + 0x18, core.getPC64());
    }

    @Test
    public void testInstructionBreakpointDeliversDebugException() throws Exception
    {
        X86Core core = core32(
                0xB8, 0x20, 0x00, 0x01, 0x00,           // mov eax, 0x10020
                0x0F, 0x23, 0xC0,                       // mov dr0, eax
                0xB8, 0x02, 0x02, 0x00, 0x00,           // mov eax, 0x202 (G0, GE, execute)
                0x0F, 0x23, 0xF8,                       // mov dr7, eax
                0xEB, 0x0E);                            // jmp 0x10020
        load(core, 0x10020, 0x43);                      // inc ebx
        installDebugGate(core);
        steps(core, 5);
        assertEquals(0x10020, core.getPC64());

        core.step();                                    // #DB fault: before the instruction
        assertEquals(HANDLER, core.getPC64());
        assertEquals(0x8000 - 12, core.getReg64(X86Core.REG_RSP));
        assertEquals(0x10020, core.readMem32(0x8000 - 12));
        assertEquals(0x08, core.readMem32(0x8000 - 8));
        assertEquals(1, core.getDebugReg(6) & 0xF);
        assertEquals(0, core.getReg64(X86Core.REG_RBX));

        core.step();                                    // iretd resumes at the faulting instruction
        assertEquals(0x10020, core.getPC64());
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));
        assertTrue("RF set on resume", (core.getRflags64() & (1 << 16)) != 0);

        core.step();                                    // RF suppresses the same breakpoint
        assertEquals(0x10021, core.getPC64());
        assertEquals(1, core.getReg64(X86Core.REG_RBX));
        assertEquals(0, core.getRflags64() & (1 << 16));
    }

    @Test
    public void testNullInstructionBreakpointDeliversDebugException() throws Exception
    {
        // The exact jxcore setup: an execute breakpoint on address 0, then a
        // jump through a null pointer.  The emulator must deliver #DB instead
        // of stopping with "the #DB exception is not emulated".
        X86Core core = core32(
                0x31, 0xC0,                             // xor eax, eax
                0x0F, 0x23, 0xC0,                       // mov dr0, eax
                0xB8, 0x02, 0x02, 0x00, 0x00,           // mov eax, 0x202 (G0, GE, execute)
                0x0F, 0x23, 0xF8,                       // mov dr7, eax
                0x31, 0xDB,                             // xor ebx, ebx
                0xFF, 0xE3);                            // jmp ebx (null pointer)
        load(core, 0, 0x90);                            // nop at address 0
        installDebugGate(core);
        steps(core, 6);
        assertEquals(0, core.getPC64());

        core.step();                                    // #DB instead of an unknown-opcode stop
        assertEquals(HANDLER, core.getPC64());
        assertEquals(0, core.readMem32(0x8000 - 12));
        assertEquals(1, core.getDebugReg(6) & 0xF);

        core.step();                                    // iretd
        assertEquals(0, core.getPC64());
        assertEquals(0x8000, core.getReg64(X86Core.REG_RSP));

        core.step();                                    // RF suppresses the breakpoint at 0
        assertEquals(1, core.getPC64());
        assertEquals(0, core.getRflags64() & (1 << 16));
    }

    @Test
    public void testPagingOutsideLongModeStops() throws Exception
    {
        X86Core core = core32(
                0x0F, 0x20, 0xC0,                       // mov eax, cr0
                0x0D, 0x00, 0x00, 0x00, 0x80,           // or eax, 0x80000000
                0x0F, 0x22, 0xC0);                      // mov cr0, eax
        String message = unknown(core, 3);
        assertTrue(message, message.contains("turns on paging outside long mode"));
        assertEquals(0, core.getCr0() & X86Core.CR0_PG);
    }
}

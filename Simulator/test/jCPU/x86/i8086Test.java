package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class i8086Test {
    @Test
    public void testNopAdvancesPC() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        core.writeMem8(0, (byte)0x90);
        int pcBefore = core.getPC();
        core.step();
        assertEquals(pcBefore + 1, core.getPC());
    }

    @Test
    public void testMovImmToReg() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 0x1234 (opcode B8, followed by 0x34, 0x12)
        core.writeMem8(0, (byte)0xB8);
        core.writeMem8(1, (byte)0x34);
        core.writeMem8(2, (byte)0x12);
        core.step();
        assertEquals(0x1234, core.getReg(X86Core.REG_EAX));
        assertEquals(3, core.getPC());
    }

    @Test
    public void testMovRegToReg() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 0xABCD
        core.writeMem8(0, (byte)0xB8);
        core.writeMem8(1, (byte)0xCD);
        core.writeMem8(2, (byte)0xAB);
        core.step();
        // MOV BX, AX (opcode 89, modrm C3: mod=11, reg=011(BX), r/m=000(AX))
        core.writeMem8(3, (byte)0x89);
        core.writeMem8(4, (byte)0xC3);
        core.step();
        assertEquals(0xABCD, core.getReg(X86Core.REG_EBX));
    }

    @Test
    public void testPushPop() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 0x5678
        core.writeMem8(0, (byte)0xB8);
        core.writeMem8(1, (byte)0x78);
        core.writeMem8(2, (byte)0x56);
        core.step();
        // PUSH AX (opcode 50)
        core.writeMem8(3, (byte)0x50);
        int spBefore = core.getReg(X86Core.REG_ESP);
        core.step();
        assertEquals(spBefore - 4, core.getReg(X86Core.REG_ESP));
        // POP BX (opcode 5B)
        core.writeMem8(4, (byte)0x5B);
        core.step();
        assertEquals(0x5678, core.getReg(X86Core.REG_EBX));
    }

    @Test
    public void testAddRegToReg() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 5
        core.writeMem8(0, (byte)0xB8); core.writeMem8(1, (byte)5); core.writeMem8(2, (byte)0);
        core.step();
        // MOV BX, 3
        core.writeMem8(3, (byte)0xBB); core.writeMem8(4, (byte)3); core.writeMem8(5, (byte)0);
        core.step();
        // ADD AX, BX (opcode 01, modrm D8: mod=11, reg=011(BX), r/m=000(AX))
        core.writeMem8(6, (byte)0x01); core.writeMem8(7, (byte)0xD8);
        core.step();
        assertEquals(8, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testSubRegToReg() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 10
        core.writeMem8(0, (byte)0xB8); core.writeMem8(1, (byte)10); core.writeMem8(2, (byte)0);
        core.step();
        // MOV BX, 3
        core.writeMem8(3, (byte)0xBB); core.writeMem8(4, (byte)3); core.writeMem8(5, (byte)0);
        core.step();
        // SUB AX, BX (opcode 29, modrm D8: mod=11, reg=011(BX), r/m=000(AX))
        core.writeMem8(6, (byte)0x29); core.writeMem8(7, (byte)0xD8);
        core.step();
        assertEquals(7, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testIncDecReg() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 5
        core.writeMem8(0, (byte)0xB8); core.writeMem8(1, (byte)5); core.writeMem8(2, (byte)0);
        core.step();
        // INC AX (opcode 40)
        core.writeMem8(3, (byte)0x40);
        core.step();
        assertEquals(6, core.getReg(X86Core.REG_EAX));
        // DEC AX (opcode 48)
        core.writeMem8(4, (byte)0x48);
        core.step();
        assertEquals(5, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testConditionalJumps() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // JMP short +3 (opcode EB, rel=3)
        core.writeMem8(0, (byte)0xEB); core.writeMem8(1, (byte)3);
        // NOP at offset 2 (should be skipped)
        core.writeMem8(2, (byte)0x90);
        // Target at PC=5: MOV AX, 0x42
        core.writeMem8(5, (byte)0xB8); core.writeMem8(6, (byte)0x42); core.writeMem8(7, (byte)0);
        core.step();
        assertEquals(5, core.getPC());
        core.step();
        assertEquals(0x42, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testCallAndRet() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // At PC=0: CALL +3 (opcode E8, rel16 = 3, target = 0+3+3 = 6)
        core.writeMem8(0, (byte)0xE8); core.writeMem8(1, (byte)3); core.writeMem8(2, (byte)0);
        // At PC=3: MOV AX, 0 (should be skipped during call)
        core.writeMem8(3, (byte)0xB8); core.writeMem8(4, (byte)0); core.writeMem8(5, (byte)0);
        // At PC=6 (target): MOV AX, 0x99
        core.writeMem8(6, (byte)0xB8); core.writeMem8(7, (byte)0x99); core.writeMem8(8, (byte)0);
        // At PC=9: RET (opcode C3)
        core.writeMem8(9, (byte)0xC3);
        // Step CALL
        core.step();
        assertEquals(6, core.getPC());
        // Step MOV AX, 0x99
        core.step();
        assertEquals(0x99, core.getReg(X86Core.REG_EAX));
        // Step RET
        core.step();
        assertEquals(3, core.getPC());
    }

    @Test
    public void testAndOrXor() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 0xFF
        core.writeMem8(0, (byte)0xB8); core.writeMem8(1, (byte)0xFF); core.writeMem8(2, (byte)0);
        core.step();
        // AND AX, 0x0F (opcode 25, imm16)
        core.writeMem8(3, (byte)0x25); core.writeMem8(4, (byte)0x0F); core.writeMem8(5, (byte)0);
        core.step();
        assertEquals(0x0F, core.getReg(X86Core.REG_EAX));
        // OR AX, 0xF0 (opcode 0D, imm16)
        core.writeMem8(6, (byte)0x0D); core.writeMem8(7, (byte)0xF0); core.writeMem8(8, (byte)0);
        core.step();
        assertEquals(0xFF, core.getReg(X86Core.REG_EAX));
        // XOR AX, AX (opcode 31, modrm C0)
        core.writeMem8(9, (byte)0x31); core.writeMem8(10, (byte)0xC0);
        core.step();
        assertEquals(0, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testMemoryMov() throws Exception {
        X86Core core = new X86Core(64 * 1024);
        core.setActiveExecutor(new i8086());
        // MOV AX, 0x42
        core.writeMem8(0, (byte)0xB8); core.writeMem8(1, (byte)0x42); core.writeMem8(2, (byte)0);
        core.step();
        // MOV [2000], AX (opcode 89, modrm 06: mod=00, reg=000(AX), r/m=110=direct addr)
        core.writeMem8(3, (byte)0x89); core.writeMem8(4, (byte)0x06);
        core.writeMem8(5, (byte)0xD0); core.writeMem8(6, (byte)0x07);
        // Step MOV
        int pcBefore = core.getPC();
        core.step();
        assertEquals(pcBefore + 4, core.getPC());
        // Read back from memory
        assertEquals(0x42, core.readMem16(0x07D0));
    }
}

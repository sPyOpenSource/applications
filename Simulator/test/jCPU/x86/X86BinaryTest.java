package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86BinaryTest {
    private X86Core makeCore(byte[] code) {
        X86Core core = new X86Core(1024);
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(i, code[i] & 0xFF);
        }
        core.setPC(0);
        return core;
    }

    private void step(X86Core core) throws Exception {
        core.step();
    }

    @Test
    public void testMovImmReg() throws Exception {
        byte[] code = {(byte)0xB8, 0x05, 0x00, 0x00, 0x00};
        X86Core core = makeCore(code);
        core.setActiveExecutor(new X86BinaryExecutor());
        step(core);
        assertEquals(5, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testMovRegToReg() throws Exception {
        byte[] code = {
            (byte)0xB8, 0x05, 0x00, 0x00, 0x00,
            (byte)0x89, (byte)0xC3
        };
        X86Core core = makeCore(code);
        core.setActiveExecutor(new X86BinaryExecutor());
        step(core);
        assertEquals(5, core.getReg(X86Core.REG_EAX));
        step(core);
        assertEquals(5, core.getReg(X86Core.REG_EBX));
    }

    @Test
    public void testAddRegToReg() throws Exception {
        byte[] code = {
            (byte)0xB8, 0x05, 0x00, 0x00, 0x00,
            (byte)0xBB, 0x03, 0x00, 0x00, 0x00,
            (byte)0x01, (byte)0xD8
        };
        X86Core core = makeCore(code);
        core.setActiveExecutor(new X86BinaryExecutor());
        step(core);
        assertEquals(5, core.getReg(X86Core.REG_EAX));
        step(core);
        assertEquals(3, core.getReg(X86Core.REG_EBX));
        step(core);
        assertEquals(8, core.getReg(X86Core.REG_EAX));
        assertFalse(core.getFlag(X86Core.FLAG_ZF));
        assertFalse(core.getFlag(X86Core.FLAG_SF));
        assertFalse(core.getFlag(X86Core.FLAG_CF));
        assertFalse(core.getFlag(X86Core.FLAG_OF));
    }

    @Test
    public void testAddSetsZeroFlag() throws Exception {
        byte[] code = {
            (byte)0xB8, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xBB, 0x01, 0x00, 0x00, 0x00,
            (byte)0x01, (byte)0xD8
        };
        X86Core core = makeCore(code);
        core.setActiveExecutor(new X86BinaryExecutor());
        step(core); step(core); step(core);
        assertEquals(0, core.getReg(X86Core.REG_EAX));
        assertTrue(core.getFlag(X86Core.FLAG_ZF));
    }
}

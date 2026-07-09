package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86RoundTripTest {
    @Test
    public void testAddSubRoundTrip() throws Exception {
        byte[] code = {
            (byte)0xB8, 0x05, 0x00, 0x00, 0x00,
            (byte)0xBB, 0x03, 0x00, 0x00, 0x00,
            (byte)0x01, (byte)0xD8,
            (byte)0x29, (byte)0xD8
        };
        X86Core core = new X86Core(1024);
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(i, code[i] & 0xFF);
        }
        core.setPC(0);
        core.setActiveExecutor(new X86BinaryExecutor());

        int[] regsBefore = new int[X86Core.REG_COUNT];
        for (int i = 0; i < X86Core.REG_COUNT; i++) regsBefore[i] = core.getReg(i);

        for (int i = 0; i < 4; i++) core.step();

        assertEquals(5, core.getReg(X86Core.REG_EAX));
        assertEquals(3, core.getReg(X86Core.REG_EBX));
    }

    @Test
    public void testPushPopRoundTrip() throws Exception {
        byte[] code = {
            (byte)0xB8, 0x34, 0x12, 0x00, 0x00,
            (byte)0x50,
            (byte)0x58
        };
        X86Core core = new X86Core(1024);
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(i, code[i] & 0xFF);
        }
        int espBefore = core.getReg(X86Core.REG_ESP);
        core.setPC(0);
        core.setActiveExecutor(new X86BinaryExecutor());

        core.step();
        assertEquals(0x1234, core.getReg(X86Core.REG_EAX));

        core.step();
        assertEquals(espBefore - 4, core.getReg(X86Core.REG_ESP));
        assertEquals(0x1234, core.readMem32(core.getReg(X86Core.REG_ESP)));

        core.step();
        assertEquals(espBefore, core.getReg(X86Core.REG_ESP));
        assertEquals(0x1234, core.getReg(X86Core.REG_EAX));
    }

    @Test
    public void testConditionalJumpRoundTrip() throws Exception {
        byte[] code = {
            (byte)0xB8, 0x05, 0x00, 0x00, 0x00,
            (byte)0xBB, 0x03, 0x00, 0x00, 0x00,
            (byte)0x39, (byte)0xD8,
            (byte)0x74, 0x04,
            (byte)0xB8, 0x01, 0x00, 0x00, 0x00,
            (byte)0xEB, 0x02,
            (byte)0xB8, 0x02, 0x00, 0x00, 0x00,
        };
        X86Core core = new X86Core(1024);
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(i, code[i] & 0xFF);
        }
        core.setPC(0);
        core.setActiveExecutor(new X86BinaryExecutor());

        for (int i = 0; i < 6; i++) core.step();

        assertEquals(1, core.getReg(X86Core.REG_EAX));
        assertEquals(3, core.getReg(X86Core.REG_EBX));
    }
}

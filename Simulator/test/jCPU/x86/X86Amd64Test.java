package jCPU.x86;

import org.junit.Test;
import static org.junit.Assert.*;

public class X86Amd64Test {
    private static final int START = 0x1000;

    private static X86Core coreWith(int... code) {
        X86Core core = new X86Core(64 * 1024);
        core.setLongMode(true);
        core.setActiveExecutor(new X86Amd64());
        for (int i = 0; i < code.length; i++) {
            core.writeMem8(START + i, code[i]);
        }
        core.setPC64(START);
        return core;
    }

    private static X86UnknownOpcodeException stepExpectingUnknown(X86Core core) throws Exception {
        try {
            core.step();
        } catch (X86UnknownOpcodeException ex) {
            return ex;
        }
        fail("expected X86UnknownOpcodeException, RIP is now 0x" + Long.toHexString(core.getPC64()));
        return null;
    }

    @Test
    public void testNopStillAdvances() throws Exception {
        X86Core core = coreWith(0x90);
        core.step();
        assertEquals(START + 1, core.getPC64());
    }

    @Test
    public void testUnknownOpcodeStopsOnTheInstruction() throws Exception {
        // 0xD4 (AAM) does not exist in 64-bit mode.
        X86Core core = coreWith(0xD4, 0x0A);
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertEquals(START, ex.getRip());
        assertEquals("RIP must stay on the unknown instruction", START, core.getPC64());
    }

    @Test
    public void testMessageNamesAddressOpcodeAndBytes() throws Exception {
        X86Core core = coreWith(0xD4, 0x0A);
        String msg = stepExpectingUnknown(core).getMessage();
        assertTrue(msg, msg.contains("RIP=0x0000000000001000"));
        assertTrue(msg, msg.contains("opcode 0xD4"));
        assertTrue(msg, msg.contains("bytes: D4 0A"));
        assertTrue(msg, msg.contains("RAX="));
        assertTrue(msg, msg.contains("RFLAGS="));
    }

    @Test
    public void testUnknownOpcodeAfterRexReportsPrefixAddress() throws Exception {
        // REX.W + 0xD4: the reported address is that of the REX prefix.
        X86Core core = coreWith(0x48, 0xD4);
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertEquals(START, ex.getRip());
        assertEquals(START, core.getPC64());
    }

    @Test
    public void testUnknownTwoByteOpcode() throws Exception {
        // 0x0F 0x0B (UD2)
        X86Core core = coreWith(0x0F, 0x0B);
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertTrue(ex.getMessage(), ex.getMessage().contains("opcode 0x0F 0x0B"));
        assertEquals(START, core.getPC64());
    }

    @Test
    public void testUnimplementedGroupExtension() throws Exception {
        // 0xFE /2 does not exist; ModR/M 0xD0: mod=11, reg=010, rm=000
        X86Core core = coreWith(0xFE, 0xD0);
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertTrue(ex.getMessage(), ex.getMessage().contains("opcode 0xFE /2"));
        assertEquals(START, core.getPC64());
    }

    @Test
    public void testUnimplementedFarCall() throws Exception {
        // 0xFF /3 (far CALL) with ModR/M 0x18: mod=00, reg=011, rm=000
        X86Core core = coreWith(0xFF, 0x18);
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertTrue(ex.getMessage(), ex.getMessage().contains("opcode 0xFF /3"));
        assertEquals(START, core.getPC64());
    }

    @Test
    public void testGoStopsAtUnknownOpcode() throws Exception {
        // NOP, NOP, then an unknown opcode: go() must not run past it.
        X86Core core = coreWith(0x90, 0x90, 0xD4);
        try {
            core.go(100);
            fail("expected X86UnknownOpcodeException");
        } catch (X86UnknownOpcodeException ex) {
            assertEquals(START + 2, ex.getRip());
        }
        assertEquals(START + 2, core.getPC64());
    }

    @Test
    public void testMessageListsInstructionsThatLedThere() throws Exception {
        // nop; jmp +2 (over two bytes); AAM
        X86Core core = coreWith(0x90, 0xEB, 0x02, 0x90, 0x90, 0xD4, 0x0A);
        core.step();
        core.step();
        X86UnknownOpcodeException ex = stepExpectingUnknown(core);
        assertEquals(START + 5, ex.getRip());
        assertArrayEquals(new long[] {START, START + 1}, ex.getRecentInstructions());
        String msg = ex.getMessage();
        assertTrue(msg, msg.contains("Last 2 instructions before this one"));
        assertTrue(msg, msg.contains("RIP=0x0000000000001001  EB 02"));
    }

    @Test
    public void testHistoryKeepsOnlyTheMostRecentInstructions() throws Exception {
        int[] code = new int[21];
        java.util.Arrays.fill(code, 0x90);
        code[20] = 0xD4;
        X86Core core = coreWith(code);
        for (int i = 0; i < 20; i++) {
            core.step();
        }
        long[] recent = stepExpectingUnknown(core).getRecentInstructions();
        assertEquals(16, recent.length);
        assertEquals(START + 4, recent[0]);
        assertEquals(START + 19, recent[15]);
    }

    @Test
    public void testNoHistoryLineWhenNothingRanBefore() throws Exception {
        X86Core core = coreWith(0xD4, 0x0A);
        String msg = stepExpectingUnknown(core).getMessage();
        assertFalse(msg, msg.contains("instructions before this one"));
    }
}

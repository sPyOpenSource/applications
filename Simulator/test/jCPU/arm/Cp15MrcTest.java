package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Cp15MrcTest {

    @Test
    public void testMrcTtbrReturnsWrittenValue() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(0, 0x12345);
        cpu.executeARM(0xEE020F10); // mcr p15, 0, r0, c2, c0, 0 (TTBR0)
        cpu.executeARM(0xEE121F10); // mrc p15, 0, r1, c2, c0, 0

        assertEquals(0x12345, cpu.r(1));
    }

    @Test
    public void testMrcDacrReturnsWrittenValue() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(0, 0xFF);
        cpu.executeARM(0xEE030F10); // mcr p15, 0, r0, c3, c0, 0 (DACR)
        cpu.executeARM(0xEE131F10); // mrc p15, 0, r1, c3, c0, 0

        assertEquals(0xFF, cpu.r(1));
    }

    @Test
    public void testMrcGenericTimerDoesNotFail() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.executeARM(0xEE1E1F10); // mrc p15, 0, r1, c14, c0, 0 (CNTFRQ)
    }

    @Test
    public void testMcrMemoryRemapRegisters() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(3, (int) 0xFF0A81A8L); // typical Linux PRRR value
        cpu.executeARM(0xEE0A3F12); // mcr p15, 0, r3, c10, c2, 0 (PRRR)
        cpu.executeARM(0xEE1A1F12); // mrc p15, 0, r1, c10, c2, 0
        assertEquals((int) 0xFF0A81A8L, cpu.r(1));

        cpu.executeARM(0xEE0A3F13); // mcr p15, 0, r3, c10, c3, 0 (legacy alias)
        cpu.executeARM(0xEE0A3F14); // mcr p15, 0, r3, c10, c4, 0 (MAIR0)
        cpu.executeARM(0xEE0A3F34); // mcr p15, 0, r3, c10, c4, 1 (MAIR1)

        cpu.r(6, (int) 0x40E040E0L); // canonical Linux NMRR value
        cpu.executeARM(0xEE0A6F32); // mcr p15, 0, r6, c10, c2, 1 (NMRR)
        cpu.executeARM(0xEE1A1F32); // mrc p15, 0, r1, c10, c2, 1
        assertEquals((int) 0x40E040E0L, cpu.r(1));
    }
}

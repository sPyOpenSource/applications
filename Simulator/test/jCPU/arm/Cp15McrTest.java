package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Cp15McrTest {

    @Test
    public void testMcrWritesSctlr() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(0, 0x7); // enable M, A and C bits
        cpu.executeARM(0xEE010F10); // mcr p15, 0, r0, c1, c0, 0
        cpu.executeARM(0xEE111F10); // mrc p15, 0, r1, c1, c0, 0

        assertEquals(0x7, cpu.r(1) & 0x7);
    }

    @Test
    public void testMcrCacheAndTlbMaintenanceAreNoOps() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.executeARM(0xEE070F15); // mcr p15, 0, r0, c7, c5, 0 (ICIALLU)
        cpu.executeARM(0xEE080F17); // mcr p15, 0, r0, c8, c7, 0 (TLBIALL)
    }

    @Test
    public void testMcrTtbrIsIgnored() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.executeARM(0xEE020F10); // mcr p15, 0, r0, c2, c0, 0 (TTBR0)
    }
}

package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** CP14 (debug/Jazelle) is not modelled; accesses must not fault the kernel. */
public class Cp14PermissiveTest {

    @Test
    public void testMrcP14ReturnsZeroWithoutFault() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.executeARM(0xEE100E10); // mrc p14, #0, r0, c0, c0, #0
        assertEquals(0, cpu.r(0));
    }

    @Test
    public void testMcrP14IsNoOpWithoutFault() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.r(0, 0xDEADBEEF);
        cpu.executeARM(0xEE000E10); // mcr p14, #0, r0, c0, c0, #0
    }

    @Test
    public void testCdpP14IsNoOpWithoutFault() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.executeARM(0xEE000E00); // cdp p14, #0, c0, c0, c0, #0
    }

    @Test
    public void testMrcP14DebugIdReturnsZero() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.executeARM(0xEE101E10); // mrc p14, #0, r1, c0, c0, #0 (DBGDIDR)
        assertEquals(0, cpu.r(1));
    }
}

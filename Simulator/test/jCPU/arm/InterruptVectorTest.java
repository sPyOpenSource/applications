package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class InterruptVectorTest {

    @Test
    public void testVectorAtLowAddressByDefault() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        // EXCEPTION_VECTOR_PREFETCH_ABORT = 3 -> offset 0xC
        assertEquals(0xC, cpu.getInterruptVector(ARMConstants.EXCEPTION_VECTOR_PREFETCH_ABORT));
    }

    @Test
    public void testVectorUsesVbarWhenProgrammed() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        // mcr p15, 0, r0, c12, c0, 0 -- write VBAR = 0x8000
        cpu.r(0, 0x8000);
        cpu.executeARM(0xEE0C0F10);

        assertEquals(0x8000 + 0xC, cpu.getInterruptVector(ARMConstants.EXCEPTION_VECTOR_PREFETCH_ABORT));
    }

    @Test
    public void testVectorUsesHighVectorsWhenVSet() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        // Set SCTLR.V (bit 13) directly via MCR, preserving other bits
        cpu.r(0, cpu.cp15.SCTLR | (1 << CP15.SCTLR_BIT_V));
        cpu.executeARM(0xEE010F10); // mcr p15, 0, r0, c1, c0, 0

        assertEquals(0xFFFF000C, cpu.getInterruptVector(ARMConstants.EXCEPTION_VECTOR_PREFETCH_ABORT));
    }
}

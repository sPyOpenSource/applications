package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class CpuTotalCyclesTest {
    @Test
    public void testTotalCyclesIncrementsPerInstruction() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.execute();
        assertEquals(1, cpu.getTotalCycles());
        cpu.execute();
        assertEquals(2, cpu.getTotalCycles());
    }
}

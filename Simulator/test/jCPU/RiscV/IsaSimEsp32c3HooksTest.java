package jCPU.RiscV;

import static org.junit.Assert.*;
import org.junit.Test;

public class IsaSimEsp32c3HooksTest {

    // ADDI x1, x0, 1  (opcode 0x13 OP_IMM, funct3 0, imm = 1)
    private static final int ADDI_X1_X0_1 = 0x00100093;

    private static void writeCode(CPU sim, int addr, int word) {
        sim.code.write(addr, (byte) word);
        sim.code.write(addr + 1, (byte) (word >>> 8));
        sim.code.write(addr + 2, (byte) (word >>> 16));
        sim.code.write(addr + 3, (byte) (word >>> 24));
    }

    @Test
    public void testSetMemoryMapStoresReference() {
        CPU sim = new CPU();
        Esp32c3MemoryMap map = new Esp32c3MemoryMap();
        sim.setMemoryMap(map);
        assertSame(map, sim.memoryMap);
    }

    @Test
    public void testResetSetsPcToResetVector() {
        CPU sim = new CPU();
        sim.pc = 0x12345678; // perturb
        sim.reset();
        assertEquals(Esp32c3MemoryMap.RESET_VECTOR, sim.pc());
    }

    @Test
    public void testResetClearsRegisters() {
        CPU sim = new CPU();
        sim.reg[1] = 42;
        sim.reg[5] = -7;
        sim.reg[31] = Integer.MAX_VALUE;
        sim.reset();
        for (int i = 0; i < 32; i++) {
            assertEquals(0, sim.reg[i]);
        }
    }

    @Test
    public void testResetClearsClock() {
        CPU sim = new CPU();
        sim.cc = 123; // perturb
        sim.reset();
        assertEquals(0, sim.getClock());
    }

    @Test
    public void testPcGetter() {
        CPU sim = new CPU();
        sim.pc = Esp32c3MemoryMap.RESET_VECTOR;
        assertEquals(Esp32c3MemoryMap.RESET_VECTOR, sim.pc());
    }

    @Test
    public void testClockIncrementsPerStep() throws Exception {
        CPU sim = new CPU();
        sim.reset();
        int base = Esp32c3MemoryMap.RESET_VECTOR;
        writeCode(sim, base, ADDI_X1_X0_1);
        writeCode(sim, base + 4, ADDI_X1_X0_1);

        assertEquals(0, sim.getClock());
        assertEquals(base, sim.pc());

        sim.step();
        assertEquals(1, sim.getClock());
        assertEquals(base + 4, sim.pc());
    }
}

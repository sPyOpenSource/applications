package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class ArmDataProcessingTest {
    @Test
    public void testMovImmediate() throws Exception {
        CPU cpu = new CPU();
        cpu.executeARM(0xE3A00042); // MOV R0, #0x42
        assertEquals(0x42, cpu.r(0));
    }

    @Test
    public void testAddImmediate() throws Exception {
        CPU cpu = new CPU();
        cpu.executeARM(0xE3A00042); // MOV R0, #0x42
        cpu.executeARM(0xE2800001); // ADD R0, R0, #1
        assertEquals(0x43, cpu.r(0));
    }

    @Test
    public void testCmpSetsZ() throws Exception {
        CPU cpu = new CPU();
        cpu.executeARM(0xE3A00001); // MOV R0, #1
        cpu.executeARM(0xE3500001); // CMP R0, #1
        assertTrue(cpu.conditionZ());
    }
}

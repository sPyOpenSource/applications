package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class ArmMultiplyTest {
    @Test
    public void testMul() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.writeRegister(0, 6);
        cpu.writeRegister(1, 7);
        // MUL R2, R0, R1  => 6 * 7 = 42  (decoder encoding: op2==9, op1==0)
        cpu.executeARM(0xE0020091);
        assertEquals(42, cpu.r(2));
    }

    @Test
    public void testMla() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        cpu.writeRegister(0, 6);
        cpu.writeRegister(1, 7);
        cpu.writeRegister(2, 10);
        // MLA R3, R0, R1, R2  => 6 * 7 + 10 = 52  (decoder encoding: op2==9, op1==2)
        cpu.executeARM(0xE0232190);
        assertEquals(52, cpu.r(3));
    }
}

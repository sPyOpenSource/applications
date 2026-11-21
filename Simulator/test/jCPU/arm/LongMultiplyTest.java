package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** 64-bit multiply / multiply-accumulate instructions (UMULL, UMLAL, SMULL, SMLAL, UMAAL). */
public class LongMultiplyTest {

    private static CPU cpu() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        return cpu;
    }

    @Test
    public void testUmlalFaultingInstruction() throws Exception {
        // Exact kernel instruction: umlal r1, r0, r3, r4  (RdHi=r0, RdLo=r1, Rn=r3, Rm=r4)
        CPU cpu = cpu();
        cpu.writeRegister(3, 0x10000);
        cpu.writeRegister(4, 0x10000);          // product = 0x1_00000000
        cpu.writeRegister(0, 0);               // RdHi
        cpu.writeRegister(1, 5);               // RdLo  (accumulator = 5)
        cpu.executeARM(0xE0A01493);            // UMLAL
        assertEquals(5, cpu.r(1));             // low word
        assertEquals(1, cpu.r(0));             // high word
    }

    @Test
    public void testUmlal() throws Exception {
        // umlal r1, r4, r3, r0  (RdHi=r4, RdLo=r1, Rn=r3, Rm=r0)
        CPU cpu = cpu();
        cpu.writeRegister(3, 7);
        cpu.writeRegister(0, 100000);          // product = 700000
        cpu.writeRegister(4, 0);
        cpu.writeRegister(1, 10);              // accumulator = 10
        cpu.executeARM(0xE0A41093);            // UMLAL
        assertEquals(700010, cpu.r(1));
        assertEquals(0, cpu.r(4));
    }

    @Test
    public void testSmull() throws Exception {
        // smull r1, r4, r3, r0  => r4:r1 = (int)r3 * (int)r0
        CPU cpu = cpu();
        cpu.writeRegister(3, 0xFFFFFFFF);      // -1
        cpu.writeRegister(0, 2);               // product = -2
        cpu.executeARM(0xE0C41093);            // SMULL
        assertEquals(0xFFFFFFFE, cpu.r(1));
        assertEquals(0xFFFFFFFF, cpu.r(4));
    }

    @Test
    public void testSmlal() throws Exception {
        // smlal r1, r4, r3, r0  => r4:r1 = (int)r3 * (int)r0 + r4:r1
        CPU cpu = cpu();
        cpu.writeRegister(3, 0xFFFFFFFF);      // -1
        cpu.writeRegister(0, 2);               // product = -2
        cpu.writeRegister(4, 0);
        cpu.writeRegister(1, 5);               // accumulator = 5
        cpu.executeARM(0xE0E41093);            // SMLAL
        assertEquals(3, cpu.r(1));
        assertEquals(0, cpu.r(4));
    }

    @Test
    public void testUmaal() throws Exception {
        // umaal r1, r4, r3, r0  => r4:r1 = r3*r0 + r1 + r4
        CPU cpu = cpu();
        cpu.writeRegister(3, 0x10000);
        cpu.writeRegister(0, 0x10000);         // product = 0x1_00000000
        cpu.writeRegister(4, 3);
        cpu.writeRegister(1, 4);               // result = 0x1_00000007
        cpu.executeARM(0xE0441093);            // UMAAL
        assertEquals(7, cpu.r(1));
        assertEquals(1, cpu.r(4));
    }

    @Test
    public void testSmullsSetsFlags() throws Exception {
        CPU cpu = cpu();
        cpu.writeRegister(3, 0xFFFFFFFF);      // -1
        cpu.writeRegister(0, 2);               // product = -2
        cpu.executeARM(0xE0D41093);            // SMULLS
        assertTrue(cpu.conditionN());
        assertFalse(cpu.conditionZ());
    }
}

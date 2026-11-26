package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class ArmMsrTest {

    @Test
    public void testMsrRegisterWritesRegisterValueToCpsrControlField() throws Exception {
        CPU cpu = new CPU();
        cpu.reset(); // starts in SUPERVISOR (0x13)

        cpu.r(0, 0x12); // IRQ mode
        cpu.executeARM(0xE121F000); // msr cpsr_c, r0

        assertEquals(0x12, cpu.getMode());
    }

    @Test
    public void testMsrRegisterUsesRmNotItsNumber() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(3, 0xD3); // I+F set, SVC mode
        cpu.executeARM(0xE121F003); // msr cpsr_c, r3

        assertEquals(0x13, cpu.getMode());
        assertEquals(0xD3, cpu.readCPSR() & 0xFF);
    }

    @Test
    public void testMsrRegisterAllFields() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.r(0, 0x1F); // SYSTEM mode
        cpu.executeARM(0xE12FF000); // msr cpsr_cxsf, r0

        assertEquals(0x1F, cpu.getMode());
    }
}

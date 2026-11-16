package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class ArmBranchTest {
    @Test
    public void testBranchForward() throws Exception {
        CPU cpu = new CPU();
        // B, imm24=2 => imm32=8; readPC()=pc+4=4, target=4+8=12
        cpu.executeARM(0xEA000002);
        assertEquals(12, cpu.readCurrentPC());
    }

    @Test
    public void testBranchLink() throws Exception {
        CPU cpu = new CPU();
        cpu.reset(); // sets mode (needed by writeLR), pc=0
        cpu.pc(4);   // simulate pc advanced past fetched instruction (address 0)
        // BL, imm24=0 => imm32=0; readPC()=pc+4=8, LR=readPC()-4=4, target=8
        cpu.executeARM(0xEB000000);
        assertEquals(4, cpu.r(14) & ~3); // LR = address of next instruction
        assertEquals(8, cpu.readCurrentPC());
    }

    @Test
    public void testBxToArm() throws Exception {
        CPU cpu = new CPU();
        cpu.writeGPR(0, 0x1000);
        // BX R0 : 0xE12FFF10, ARM jump to 0x1000, clear Thumb bit
        cpu.executeARM(0xE12FFF10);
        assertEquals(0x1000, cpu.readCurrentPC());
        assertFalse(cpu.isThumb());
    }
}

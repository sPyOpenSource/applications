package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * When the kernel enters a terminal `b .` loop before console init, the halt
 * diagnosis must expose the call path and any strings referenced by the code,
 * otherwise there is no way to tell why it stopped.
 */
public class HaltDiagnosisTest {

    @Test
    public void testSelfBranchDetection() {
        assertTrue("b .", CPU.isSelfBranch(0xEAFFFFFE));
        assertTrue("bne .", CPU.isSelfBranch(0x1AFFFFFE));
        assertFalse("subs (decompressor loop body)", CPU.isSelfBranch(0xE2544001));
        assertFalse("bne backward 4 words", CPU.isSelfBranch(0x1AFFFFFC));
        assertFalse("ordinary bl", CPU.isSelfBranch(0xEBFFFD96));
    }

    @Test
    public void testHaltDiagnosisCapturesBranchHistoryAndString() throws Exception {
        CPU cpu = new CPU(false);
        cpu.reset();

        // Write "HALTMSG\0" into guest RAM at 0x2000 through a real STR.
        String msg = "HALTMSG";
        for(int i = 0; i <= msg.length(); i += 4) {
            int word = 0;
            for(int j = 0; j < 4 && i + j < msg.length(); j++)
                word |= (msg.charAt(i + j) & 0xFF) << (j * 8);
            cpu.writeRegister(0, 0x2000 + i);
            cpu.writeRegister(1, word);
            cpu.executeARM(0xE5801000); // str r1, [r0]
        }

        // Place a branch at the reset vector (pc=0) so the normal fetch path
        // records it in the control-flow ring buffer. Branch target is
        // pc+8+imm<<2, so 0x7FE lands at 0x2000.
        cpu.writeRegister(0, 0x0000);
        cpu.writeRegister(1, 0xEA0007FE); // b 0x2000
        cpu.executeARM(0xE5801000);       // str r1, [r0]

        cpu.writeRegister(0, 0x2000);     // r0 -> string for the diagnosis
        cpu.execute();                    // fetch+execute the b, record 0x0 -> 0x2000

        assertEquals("branch target", 0x2000, cpu.readCurrentPC());

        String diag = cpu.buildHaltDiagnosis(cpu.readCurrentPC());

        assertTrue("diagnosis header missing", diag.contains("[JCPU-HALT]"));
        assertTrue("r0 string not dumped", diag.contains("HALTMSG"));
        assertTrue("branch history missing", diag.contains("[JCPU-HALT-BRANCHES]"));
        assertTrue("taken branch not recorded", diag.contains("0x00000000 -> 0x00002000"));
    }
}

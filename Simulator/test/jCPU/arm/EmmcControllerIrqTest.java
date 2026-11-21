package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerIrqTest {
    @Test
    public void testCommandCompletionSetsInterrupt() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 0); // CMD0 completes
        assertTrue((ctl.read(EmmcController.INTERRUPT) & 0x01) != 0);
        assertTrue(cpu.haveIRQ());
        // STATUS is readable through MMIO and reports no busy/error after completion
        assertEquals(0, ctl.read(EmmcController.STATUS) & (EmmcController.STATUS_BUSY | EmmcController.STATUS_ERROR));
    }

    @Test
    public void testUnsupportedCommandExposesErrorInStatus() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 48); // CMD48 unsupported
        assertTrue((ctl.read(EmmcController.STATUS) & EmmcController.STATUS_ERROR) != 0);
        assertTrue(cpu.haveIRQ());
    }

    @Test
    public void testWritingInterruptClearsIrq() {
        CPU cpu = new CPU();
        cpu.reset();
        EmmcController ctl = new EmmcController(null, cpu);
        writeWord(ctl, EmmcController.CMDCMD, 0);
        ctl.write(EmmcController.INTERRUPT, (byte) 0x01); // write-1-to-clear
        assertEquals(0, ctl.read(EmmcController.INTERRUPT) & 0x01);
        assertFalse(cpu.haveIRQ());
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}

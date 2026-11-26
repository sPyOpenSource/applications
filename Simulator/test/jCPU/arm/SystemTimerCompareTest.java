package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerCompareTest {
    @Test
    public void testCompareMatchAssertsIRQ() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer);
        writeWord(timer, SystemTimer.C0, (int) (clo + 3));

        cpu.execute();
        readClo(timer);
        cpu.execute();
        readClo(timer);
        cpu.execute();
        readClo(timer);

        int cs = readWord(timer, SystemTimer.CS);
        assertTrue("CS match flag 0 should be set, CS=0x" + Integer.toHexString(cs), (cs & 0x1) != 0);
        assertTrue("IRQ should be asserted after compare match", cpu.haveIRQ());
    }

    @Test
    public void testNoMatchBeforeCompareReached() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);

        long clo = readClo(timer);
        writeWord(timer, SystemTimer.C0, (int) (clo + 100));

        cpu.execute();
        readClo(timer);

        int cs = readWord(timer, SystemTimer.CS);
        assertEquals(0, cs & 0x1);
        assertFalse("IRQ should not be asserted before compare reached", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private int readWord(SystemTimer timer, int offset) {
        int b0 = timer.read(offset + 0) & 0xFF;
        int b1 = timer.read(offset + 1) & 0xFF;
        int b2 = timer.read(offset + 2) & 0xFF;
        int b3 = timer.read(offset + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}

package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerWfiTest {
    @Test
    public void testExecuteTicksTimer() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        cpu.setSystemTimer(timer);

        long before = timerReadLo(timer);
        cpu.execute();
        long after = timerReadLo(timer);
        assertTrue("Counter should advance after execute()", after > before);
    }

    @Test
    public void testWfiTimerFiresWhileWaiting() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        cpu.setSystemTimer(timer);

        long clo = readClo(timer); // counter = 0
        writeWord(timer, SystemTimer.C0, (int) (clo + 10));

        cpu.setWaitingForInterrupt(true);
        cpu.execute(100); // while waiting, consumes 100 cycles, timer advances to 100, match fires

        assertTrue("Timer should have fired IRQ after WFI budget consumed", cpu.haveIRQ());

        timer.write(SystemTimer.CS + 0, (byte) 0x01);
        assertFalse("IRQ should be deasserted after write-1-to-clear", cpu.haveIRQ());
    }

    private long readClo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private long timerReadLo(SystemTimer timer) {
        return readClo(timer);
    }

    private void writeWord(SystemTimer timer, int offset, int value) {
        timer.write(offset + 0, (byte) (value & 0xFF));
        timer.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        timer.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        timer.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}

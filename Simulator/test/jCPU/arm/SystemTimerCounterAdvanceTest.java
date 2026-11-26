package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerCounterAdvanceTest {
    @Test
    public void testCounterAdvancesWithCycles() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        long before = timerReadLo(timer);
        cpu.execute();
        cpu.execute();
        cpu.execute();
        long after = timerReadLo(timer);
        assertTrue("counter should advance with cycles, before=" + before + " after=" + after, after > before);
    }

    @Test
    public void testCounterDoesNotAdvanceWithoutCycles() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        SystemTimer timer = new SystemTimer(cpu);
        long before = timerReadLo(timer);
        long after = timerReadLo(timer);
        assertEquals(before, after);
    }

    private long timerReadLo(SystemTimer timer) {
        int b0 = timer.read(SystemTimer.CLO + 0) & 0xFF;
        int b1 = timer.read(SystemTimer.CLO + 1) & 0xFF;
        int b2 = timer.read(SystemTimer.CLO + 2) & 0xFF;
        int b3 = timer.read(SystemTimer.CLO + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }
}

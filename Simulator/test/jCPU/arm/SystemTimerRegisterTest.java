package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class SystemTimerRegisterTest {
    @Test
    public void testRegistersReadZeroInitially() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        assertEquals(0, timer.read(SystemTimer.CS) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CLO) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CHI) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C0) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C1) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C2) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.C3) & 0xFF);
    }

    @Test
    public void testOutOfRangeOffsetReadsZero() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        assertEquals(0, timer.read(0x1C) & 0xFF);
        assertEquals(0, timer.read(0x100) & 0xFF);
    }

    @Test
    public void testCompareRegisterWriteAndRead() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        timer.write(SystemTimer.C0 + 0, (byte) 0x12);
        timer.write(SystemTimer.C0 + 1, (byte) 0x34);
        timer.write(SystemTimer.C0 + 2, (byte) 0x56);
        timer.write(SystemTimer.C0 + 3, (byte) 0x78);
        assertEquals(0x12, timer.read(SystemTimer.C0 + 0) & 0xFF);
        assertEquals(0x34, timer.read(SystemTimer.C0 + 1) & 0xFF);
        assertEquals(0x56, timer.read(SystemTimer.C0 + 2) & 0xFF);
        assertEquals(0x78, timer.read(SystemTimer.C0 + 3) & 0xFF);
    }

    @Test
    public void testCloAndChiAreReadOnly() throws Exception {
        SystemTimer timer = new SystemTimer(null);
        timer.write(SystemTimer.CLO + 0, (byte) 0xFF);
        timer.write(SystemTimer.CHI + 0, (byte) 0xFF);
        assertEquals(0, timer.read(SystemTimer.CLO + 0) & 0xFF);
        assertEquals(0, timer.read(SystemTimer.CHI + 0) & 0xFF);
    }
}

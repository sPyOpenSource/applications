package jCPU.x86;

import static org.junit.Assert.*;

import org.junit.Test;

public class X86PitPeripheralTest
{
    /** The first TSC value at which n PIT ticks have passed. */
    private static long tscFor(long n)
    {
        return (n * X86Core.TSC_HZ + X86PitPeripheral.PIT_HZ - 1) / X86PitPeripheral.PIT_HZ;
    }

    private static void out(X86PitPeripheral pit, int port, int value)
    {
        pit.write(port, (byte) value);
    }

    private static int in(X86PitPeripheral pit, int port)
    {
        return pit.read(port) & 0xFF;
    }

    /** What GRUB does to time a wait: channel 2, mode 0, gated by port 0x61. */
    @Test
    public void testChannel2ReachesZeroAfterItsCount()
    {
        X86Core core = new X86Core(64 * 1024);
        X86PitPeripheral pit = new X86PitPeripheral(core);
        out(pit, 0x61, in(pit, 0x61) & ~3);
        out(pit, 0x43, 0xB0);                      // channel 2, LSB then MSB, mode 0
        out(pit, 0x42, 100);
        out(pit, 0x42, 0);
        core.addTimestampCounter(tscFor(1000));
        assertEquals("gate is closed, no counting", 0, in(pit, 0x61) & 0x20);

        long t0 = core.getTimestampCounter();       // 1000 PIT ticks in
        out(pit, 0x61, (in(pit, 0x61) & ~2) | 1);  // open the gate
        core.addTimestampCounter(tscFor(1099) - t0);
        assertEquals(0, in(pit, 0x61) & 0x20);
        core.addTimestampCounter(tscFor(1100) - tscFor(1099));
        assertEquals(0x20, in(pit, 0x61) & 0x20);
    }

    @Test
    public void testLatchedCountReadsLowThenHigh()
    {
        X86Core core = new X86Core(64 * 1024);
        X86PitPeripheral pit = new X86PitPeripheral(core);
        out(pit, 0x43, 0x34);                      // channel 0, LSB then MSB, mode 2
        out(pit, 0x40, 0x00);
        out(pit, 0x40, 0x10);                      // 0x1000
        core.addTimestampCounter(tscFor(0x10));
        out(pit, 0x43, 0x00);                      // latch channel 0
        core.addTimestampCounter(tscFor(100));
        int count = in(pit, 0x40) | (in(pit, 0x40) << 8);
        assertEquals(0x1000 - 0x10, count);
    }
}

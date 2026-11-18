package jCPU.x86;

import jCPU.Peripheral;

/**
 * Intel 8254 programmable interval timer (ports 0x40-0x43) and the
 * system control port B (0x61) that gates channel 2 and shows its output.
 *
 * <p>There is no wall clock: time is the core's time stamp counter, so a
 * program that waits on the PIT sees the same number of instructions pass on
 * every run.  Channels 0 and 1 have their gate tied high; no interrupts are
 * raised.</p>
 */
public class X86PitPeripheral implements Peripheral
{
    public static final long PIT_HZ = 1193182;

    private final X86Core core;
    private final Channel[] channels = { new Channel(), new Channel(), new Channel() };
    private int portB;                      // bit 0: gate 2, bit 1: speaker

    private final class Channel
    {
        int mode;
        int access = 3;                     // 1 = LSB, 2 = MSB, 3 = LSB then MSB
        int reload = 0x10000;
        boolean writeHigh;                  // next write is the MSB
        boolean readHigh;                   // next read is the MSB
        int lowByte;
        long start = -1;                    // PIT tick the count started at; -1 = not counting
        Integer latched;

        void load(int value)
        {
            reload = value == 0 ? 0x10000 : value;
            start = gate() ? now() : -1;
        }

        boolean gate()
        {
            return this != channels[2] || (portB & 1) != 0;
        }

        long elapsed()
        {
            return start < 0 ? 0 : now() - start;
        }

        int count()
        {
            long e = elapsed();
            if (mode == 2 || mode == 3) {
                return (int) (reload - e % reload) & 0xFFFF;
            }
            return (int) (reload - e) & 0xFFFF;
        }

        boolean out()
        {
            long e = elapsed();
            switch (mode) {
                case 0:                     // high once the count reached zero
                    return start >= 0 && e >= reload;
                case 2:                     // low for one tick per period
                    return start < 0 || e % reload != reload - 1;
                case 3:                     // square wave
                    return start < 0 || e % reload < (reload + 1) / 2;
                default:
                    return true;
            }
        }
    }

    public X86PitPeripheral(X86Core core)
    {
        this.core = core;
    }

    /** Elapsed PIT ticks, from the time stamp counter. */
    private long now()
    {
        long tsc = core.getTimestampCounter();
        return tsc / X86Core.TSC_HZ * PIT_HZ + tsc % X86Core.TSC_HZ * PIT_HZ / X86Core.TSC_HZ;
    }

    @Override
    public byte read(int port)
    {
        if (port == 0x61) {
            int refresh = (int) (now() / 18) & 1;                  // toggles about every 15 us
            return (byte) (portB | (refresh << 4) | (channels[2].out() ? 0x20 : 0));
        }
        if (port < 0x40 || port > 0x42) {
            return (byte) 0xFF;
        }
        Channel ch = channels[port - 0x40];
        int value = ch.latched != null ? ch.latched : ch.count();
        int b;
        if (ch.access == 1) {
            b = value;
        } else if (ch.access == 2) {
            b = value >> 8;
        } else {
            b = ch.readHigh ? value >> 8 : value;
            ch.readHigh = !ch.readHigh;
        }
        if (ch.access != 3 || !ch.readHigh) {
            ch.latched = null;
        }
        return (byte) b;
    }

    @Override
    public void write(int port, byte value)
    {
        int v = value & 0xFF;
        if (port == 0x61) {
            boolean wasGated = channels[2].gate();
            portB = v & 0x03;
            Channel ch2 = channels[2];
            if (!wasGated && ch2.gate()) {
                ch2.start = now();          // rising gate (re)starts the count
            } else if (!ch2.gate() && ch2.mode != 0) {
                ch2.start = -1;
            }
            return;
        }
        if (port == 0x43) {
            int sel = v >> 6;
            if (sel == 3) {
                return;                     // read-back command: not supported
            }
            Channel ch = channels[sel];
            int access = (v >> 4) & 3;
            if (access == 0) {
                ch.latched = ch.count();    // counter latch
                ch.readHigh = false;
                return;
            }
            ch.access = access;
            ch.mode = (v >> 1) & 7;
            if (ch.mode > 5) {
                ch.mode -= 4;               // modes 6 and 7 are 2 and 3
            }
            ch.writeHigh = false;
            ch.readHigh = false;
            ch.latched = null;
            ch.start = -1;                  // counting waits for the new count
            return;
        }
        if (port < 0x40 || port > 0x42) {
            return;
        }
        Channel ch = channels[port - 0x40];
        if (ch.access == 1) {
            ch.load(v);
        } else if (ch.access == 2) {
            ch.load(v << 8);
        } else if (!ch.writeHigh) {
            ch.lowByte = v;
            ch.writeHigh = true;
        } else {
            ch.writeHigh = false;
            ch.load(ch.lowByte | (v << 8));
        }
    }
}

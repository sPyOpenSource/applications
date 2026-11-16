package jCPU.arm;

import jCPU.Peripheral;

/**
 * BCM2835 system timer at 0x3F003000.
 * <p>
 * Free-running 64-bit counter (CLO/CHI) advancing on CPU instruction cycles,
 * with four compare channels (C0-C3) that assert IRQ on match. CS match flags
 * are write-1-to-clear. Implements {@link Comparable} so the timer can be a
 * token in {@link CPU}'s TreeSet-based IRQ line (same pattern as EmmcController).
 */
public class SystemTimer implements Peripheral, Comparable<SystemTimer> {
    private static final java.util.concurrent.atomic.AtomicLong IRQ_SEQUENCE = new java.util.concurrent.atomic.AtomicLong();
    private final long irqId = IRQ_SEQUENCE.incrementAndGet();

    public static final int CS = 0x00;
    public static final int CLO = 0x04;
    public static final int CHI = 0x08;
    public static final int C0 = 0x0C;
    public static final int C1 = 0x10;
    public static final int C2 = 0x14;
    public static final int C3 = 0x18;

    public static final long CYCLES_PER_TICK = 1;

    private final CPU cpu;
    private final byte[] regs = new byte[0x18];
    private final int[] compare = new int[] { -1, -1, -1, -1 };
    private int csFlags = 0;
    private long counter = 0;
    private long lastCycles = 0;

    public SystemTimer(CPU cpu) {
        this.cpu = cpu;
    }

    @Override
    public byte read(int offset) {
        tickTo(cpu == null ? lastCycles : cpu.getTotalCycles());
        if (offset < 0 || offset >= 0x18) {
            return 0;
        }
        switch (offset) {
            case 0x00: return (byte) (csFlags & 0xFF);
            case 0x01: return (byte) ((csFlags >> 8) & 0xFF);
            case 0x02: return (byte) ((csFlags >> 16) & 0xFF);
            case 0x03: return (byte) ((csFlags >> 24) & 0xFF);
            case 0x04: return (byte) (counter & 0xFFL);
            case 0x05: return (byte) ((counter >> 8) & 0xFFL);
            case 0x06: return (byte) ((counter >> 16) & 0xFFL);
            case 0x07: return (byte) ((counter >> 24) & 0xFFL);
            case 0x08: return (byte) ((counter >> 32) & 0xFFL);
            case 0x09: return (byte) ((counter >> 40) & 0xFFL);
            case 0x0A: return (byte) ((counter >> 48) & 0xFFL);
            case 0x0B: return (byte) ((counter >> 56) & 0xFFL);
            default:
                return regs[offset];
        }
    }

    @Override
    public void write(int offset, byte value) {
        if (offset < 0 || offset >= 0x18) {
            System.err.println("Warning: unimplemented system timer register offset 0x" + Integer.toHexString(offset));
            return;
        }
        if (offset == CS + 0) {
            int clearMask = value & 0xFF;
            for (int ch = 0; ch < 4; ch++) {
                if ((clearMask & (1 << ch)) != 0) {
                    csFlags &= ~(1 << ch);
                }
            }
            if (cpu != null && csFlags == 0) {
                cpu.clearIRQ(this);
            }
            return;
        }
        if (offset >= CLO && offset <= CHI + 3) {
            return;
        }
        if (offset >= C0 && offset <= C3 + 3) {
            regs[offset] = value;
            if ((offset & 0x03) == 0x03) {
                int ch = (offset - C0) / 4;
                compare[ch] = (regs[offset - 3] & 0xFF)
                        | ((regs[offset - 2] & 0xFF) << 8)
                        | ((regs[offset - 1] & 0xFF) << 16)
                        | ((regs[offset] & 0xFF) << 24);
                checkMatch(ch);
            }
            return;
        }
        System.err.println("Warning: unimplemented system timer register offset 0x" + Integer.toHexString(offset));
    }

    @Override
    public int compareTo(SystemTimer other) {
        return Long.compare(irqId, other.irqId);
    }

    public void tickTo(long targetCycles) {
        long ticks = (targetCycles - lastCycles) / CYCLES_PER_TICK;
        if (ticks <= 0) return;
        counter += ticks;
        lastCycles = targetCycles;
        for (int ch = 0; ch < 4; ch++) {
            checkMatch(ch);
        }
    }

    private void checkMatch(int ch) {
        if ((csFlags & (1 << ch)) == 0 && counter >= (compare[ch] & 0xFFFFFFFFL)) {
            csFlags |= (1 << ch);
            if (cpu != null) {
                cpu.setIRQ(this);
            }
        }
    }
}

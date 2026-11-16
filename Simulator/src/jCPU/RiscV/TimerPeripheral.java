package jCPU.RiscV;

import jCPU.Peripheral;

/**
 * Simple timer peripheral for the ESP32-C3 RISC-V emulation.
 *
 * <p>Register map (byte offsets):
 * <ul>
 *   <li>0: counter low byte (read-only)</li>
 *   <li>4: counter high byte (read-only)</li>
 *   <li>8: interrupt status (read) / compare low byte (write)</li>
 * </ul>
 *
 * <p>The 64-bit {@code compare} register keeps its upper 56 bits unchanged when
 * only the low byte is written (via {@link #write(int, byte)} at offset 8 or
 * {@link #writeCompareLow(int)}). This keeps both compare write paths
 * consistent.
 *
 * <p>The timer is re-armable: once {@link #clearInterrupt()} clears a pending
 * interrupt, a subsequent counter-compare match observed by {@link #tick()}
 * asserts the interrupt again.
 */
public class TimerPeripheral implements Peripheral {
    private long counter = 0;
    private long compare = 0;
    private boolean interrupt = false;

    @Override
    public byte read(int offset) {
        switch (offset) {
            case 0: return (byte) (counter & 0xFF);
            case 4: return (byte) ((counter >> 8) & 0xFF);
            case 8: return interrupt ? (byte) 0x01 : 0x00;
            default: return 0;
        }
    }

    @Override
    public void write(int offset, byte value) {
        switch (offset) {
            case 8:
                // Update the compare low byte, preserving the upper bytes.
                compare = (compare & ~0xFFL) | (value & 0xFF);
                break;
            default:
                break;
        }
    }

    /** Advances the counter by one and asserts the interrupt on compare match. Re-armable. */
    public void tick() {
        counter++;
        if (counter == compare) {
            interrupt = true;
        }
    }

    /** Returns the low byte of the counter. */
    public long readCounterLow() {
        return counter & 0xFF;
    }

    /**
     * Writes the low byte of the compare register, preserving its upper bytes.
     * Equivalent to {@link #write(int, byte)} at offset 8.
     */
    public void writeCompareLow(int value) {
        compare = (compare & ~0xFFL) | (value & 0xFF);
    }

    /** Returns whether a compare-match interrupt is pending. */
    public boolean interruptPending() {
        return interrupt;
    }

    /** Clears a pending compare-match interrupt so it can be re-armed by tick(). */
    public void clearInterrupt() {
        interrupt = false;
    }
}

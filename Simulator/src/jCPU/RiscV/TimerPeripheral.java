package jCPU.RiscV;
import jCPU.Peripheral;
public class TimerPeripheral implements Peripheral {
    private long counter = 0;
    private long compare = 0;
    private boolean interrupt = false;
    @Override public byte read(int offset) {
        switch (offset) {
            case 0: return (byte) (counter & 0xFF);
            case 4: return (byte) ((counter >> 8) & 0xFF);
            case 8: return interrupt ? (byte) 0x01 : 0x00;
            default: return 0;
        }
    }
    @Override public void write(int offset, byte value) {
        switch (offset) {
            case 8: compare = (compare & ~0xFFL) | (value & 0xFF); break;
            default: break;
        }
    }
    public void tick() { counter++; if (counter == compare) interrupt = true; }
    public long readCounterLow() { return counter & 0xFF; }
    public void writeCompareLow(int value) { compare = value & 0xFF; }
    public boolean interruptPending() { return interrupt; }
}

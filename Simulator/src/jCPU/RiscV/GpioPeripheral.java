package jCPU.RiscV;
import jCPU.Peripheral;
public class GpioPeripheral implements Peripheral {
    private int direction = 0;
    private int output = 0;
    private int input = 0;
    @Override public byte read(int offset) {
        switch (offset) {
            case 0: return (byte) (direction & 0xFF);
            case 4: return (byte) (output & 0xFF);
            case 8: return (byte) (input & 0xFF);
            default: return 0;
        }
    }
    @Override public void write(int offset, byte value) {
        switch (offset) {
            case 0: direction = value & 0xFF; break;
            case 4: output = value & 0xFF; input = value & 0xFF; break;
            default: break;
        }
    }
}

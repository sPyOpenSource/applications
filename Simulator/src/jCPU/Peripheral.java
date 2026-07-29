package jCPU;

public interface Peripheral {
    byte read(int offset);
    void write(int offset, byte value);
}

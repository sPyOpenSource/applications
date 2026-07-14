package jCPU.x86;

public interface X86Peripheral {
    byte readPort(int port);
    void writePort(int port, byte value);
}
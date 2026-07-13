package jCPU.MCS51;

public interface XdataReadListener {
    boolean xdataRead(int address, int[] value);
}

package jCPU.Wasm;

import java.util.HashMap;
import java.util.Map;

public class WasmMemory {
    private final Map<Integer, Byte> memory = new HashMap<>();
    private int minPages;
    private int maxPages;
    private static final int PAGE_SIZE = 65536;

    public WasmMemory(int minPages, int maxPages) {
        this.minPages = minPages;
        this.maxPages = maxPages;
    }

    public int getMinPages() { return minPages; }
    public int getMaxPages() { return maxPages; }

    public int grow(int additionalPages) {
        int oldPages = minPages;
        if (maxPages >= 0 && minPages + additionalPages > maxPages) {
            return -1;
        }
        minPages += additionalPages;
        return oldPages;
    }

    public int size() {
        return minPages;
    }

    public byte readByte(int addr) {
        Byte b = memory.get(addr);
        return b != null ? b : 0;
    }

    public void writeByte(int addr, byte value) {
        memory.put(addr, value);
    }

    public int readI32(int addr) {
        return (readByte(addr) & 0xFF)
             | ((readByte(addr + 1) & 0xFF) << 8)
             | ((readByte(addr + 2) & 0xFF) << 16)
             | ((readByte(addr + 3) & 0xFF) << 24);
    }

    public void writeI32(int addr, int value) {
        writeByte(addr, (byte) value);
        writeByte(addr + 1, (byte) (value >> 8));
        writeByte(addr + 2, (byte) (value >> 16));
        writeByte(addr + 3, (byte) (value >> 24));
    }

    public long readI64(int addr) {
        long lo = readI32(addr) & 0xFFFFFFFFL;
        long hi = readI32(addr + 4) & 0xFFFFFFFFL;
        return lo | (hi << 32);
    }

    public void writeI64(int addr, long value) {
        writeI32(addr, (int) value);
        writeI32(addr + 4, (int) (value >> 32));
    }

    public float readF32(int addr) {
        return Float.intBitsToFloat(readI32(addr));
    }

    public void writeF32(int addr, float value) {
        writeI32(addr, Float.floatToIntBits(value));
    }

    public double readF64(int addr) {
        return Double.longBitsToDouble(readI64(addr));
    }

    public void writeF64(int addr, double value) {
        writeI64(addr, Double.doubleToLongBits(value));
    }

    public void loadData(int offset, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            writeByte(offset + i, data[i]);
        }
    }
}

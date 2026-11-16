package jCPU.RiscV;

import jCPU.Peripheral;
import java.util.HashMap;
import java.util.Map;

/**
 * ESP32-C3 board memory map.
 * SRAM at 0x3FC80000 (32 KiB); peripherals at 0x3FF40000.
 * UART at 0x3FF40000; GPIO at 0x3FF44000; timer at 0x3FF5F000.
 * Reset vector / ROM at 0x40000000.
 */
public final class Esp32c3MemoryMap {
    public static final int SRAM_BASE = 0x3FC80000;
    public static final int SRAM_SIZE = 0x8000;
    public static final int PERIPHERAL_BASE = 0x3FF40000;
    public static final int PERIPHERAL_END = 0x3FF5FFFF;
    public static final int UART_BASE = 0x3FF40000;
    public static final int GPIO_BASE = 0x3FF44000;
    public static final int TIMER_BASE = 0x3FF5F000;
    public static final int RESET_VECTOR = 0x40000000;
    public static final int ROM_BASE = 0x40000000;

    private final byte[] sram = new byte[SRAM_SIZE];
    private final Map<Integer, Peripheral> peripherals = new HashMap<>();

    public void registerPeripheral(int base, int size, Peripheral p) {
        for (int a = base; a < base + size; a++) {
            peripherals.put(a, p);
        }
    }

    public byte readByte(int addr) throws BusErrorException {
        Peripheral p = peripherals.get(addr);
        if (p != null) {
            return p.read(addr - PERIPHERAL_BASE);
        }
        if (addr >= SRAM_BASE && addr < SRAM_BASE + SRAM_SIZE) {
            return sram[addr - SRAM_BASE];
        }
        throw new BusErrorException(addr, "read");
    }

    public void writeByte(int addr, byte value) throws BusErrorException {
        Peripheral p = peripherals.get(addr);
        if (p != null) {
            p.write(addr - PERIPHERAL_BASE, value);
            return;
        }
        if (addr >= SRAM_BASE && addr < SRAM_BASE + SRAM_SIZE) {
            sram[addr - SRAM_BASE] = value;
            return;
        }
        throw new BusErrorException(addr, "write");
    }

    public int read32(int addr) throws BusErrorException {
        return ((readByte(addr + 3) & 0xFF) << 24) |
               ((readByte(addr + 2) & 0xFF) << 16) |
               ((readByte(addr + 1) & 0xFF) << 8) |
               (readByte(addr) & 0xFF);
    }

    public void write32(int addr, int value) throws BusErrorException {
        writeByte(addr, (byte) value);
        writeByte(addr + 1, (byte) (value >> 8));
        writeByte(addr + 2, (byte) (value >> 16));
        writeByte(addr + 3, (byte) (value >> 24));
    }

    Esp32c3MemoryMap() {}
}

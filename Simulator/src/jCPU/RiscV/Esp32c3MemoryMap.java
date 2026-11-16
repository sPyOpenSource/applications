package jCPU.RiscV;
public final class Esp32c3MemoryMap {
    public static final int SRAM_BASE = 0x3FC80000;
    public static final int SRAM_SIZE = 0x8000;
    public static final int PERIPHERAL_BASE = 0x3FF40000;
    public static final int UART_BASE = 0x3FF40000;
    public static final int GPIO_BASE = 0x3FF44000;
    public static final int TIMER_BASE = 0x3FF5F000;
    public static final int RESET_VECTOR = 0x40000000;
    public static final int ROM_BASE = 0x40000000;
    private Esp32c3MemoryMap() {}
}

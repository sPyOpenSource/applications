package jCPU.arm;

/**
 * Raspberry Pi 2 board memory map.
 * RAM at 0x00000000; peripherals at 0x3F000000.
 * PL011 UART at 0x3F201000 (offset 0x201000 from peripheral base).
 */
public final class Pi2MemoryMap {
    public static final int RAM_BASE = 0x00000000;
    public static final int PERIPHERAL_BASE = 0x3F000000;
    public static final int UART_BASE = 0x3F201000;
    public static final int EMMC_BASE = 0x3F200000;
    public static final int SYSTEM_TIMER_BASE = 0x3F003000;
    private Pi2MemoryMap() {}
}

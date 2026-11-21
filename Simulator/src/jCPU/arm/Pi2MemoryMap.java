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
    /** Size of the PL011 register page. */
    public static final int UART_SIZE = 0x1000;
    /** VC bus alias used by the kernel's early serial/debug macro (before MMU setup). */
    public static final int UART_BUS_ALIAS_BASE = 0x20201000;
    /** Kernel virtual address with a 2G/2G PAGE_OFFSET (UART_BASE + 0x80000000). */
    public static final int UART_VIRT_BASE_2G = 0xBF201000;
    /** Kernel virtual address with a 3G/1G PAGE_OFFSET (UART_BASE + 0xC0000000). */
    public static final int UART_VIRT_BASE_3G = 0xFF201000;
    /** ARM fixmap virtual address used by earlycon=pl011,mmio32 before the DT
     * console is up. Linux maps the UART at FIXADDR_TOP (0xfff00000 - PAGE_SIZE)
     * via set_fixmap(FIX_EARLYCON_MEM_BASE). With no MMU in jCPU this address
     * must be aliased to the PL011 explicitly or early printk faults on its
     * first UARTFR read at 0xffeff018. */
    public static final int UART_EARLYCON_FIXMAP_BASE = 0xFFEFF000;
    public static final int EMMC_BASE = 0x3F200000;
    public static final int SYSTEM_TIMER_BASE = 0x3F003000;
    private Pi2MemoryMap() {}
}

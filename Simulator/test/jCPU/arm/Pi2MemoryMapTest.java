package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Pi2MemoryMapTest {
    @Test
    public void testRamBase() {
        assertEquals(0x00000000, Pi2MemoryMap.RAM_BASE);
    }
    @Test
    public void testPeripheralBase() {
        assertEquals(0x3F000000, Pi2MemoryMap.PERIPHERAL_BASE);
    }
    @Test
    public void testUartBase() {
        assertEquals(0x3F201000, Pi2MemoryMap.UART_BASE);
    }
    @Test
    public void testEmmcBase() {
        assertEquals(0x3F200000, Pi2MemoryMap.EMMC_BASE);
    }
}

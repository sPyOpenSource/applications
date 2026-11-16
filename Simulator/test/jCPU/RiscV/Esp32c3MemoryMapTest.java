package jCPU.RiscV;
import static org.junit.Assert.*;
import org.junit.Test;
public class Esp32c3MemoryMapTest {
    @Test public void testSramBase() { assertEquals(0x3FC80000, Esp32c3MemoryMap.SRAM_BASE); }
    @Test public void testPeripheralBase() { assertEquals(0x3FF40000, Esp32c3MemoryMap.PERIPHERAL_BASE); }
    @Test public void testUartBase() { assertEquals(0x3FF40000, Esp32c3MemoryMap.UART_BASE); }
    @Test public void testGpioBase() { assertEquals(0x3FF44000, Esp32c3MemoryMap.GPIO_BASE); }
    @Test public void testTimerBase() { assertEquals(0x3FF5F000, Esp32c3MemoryMap.TIMER_BASE); }
    @Test public void testResetVector() { assertEquals(0x40000000, Esp32c3MemoryMap.RESET_VECTOR); }
}

package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;
import jCPU.arm.UartPeripheral;
import jCPU.arm.Peripheral;

public class UartPeripheralTest {
    @Test
    public void testWrite() {
        Peripheral uart = new UartPeripheral();
        uart.write(0, (byte) 'H');
        uart.write(0, (byte) 'i');
    }

    @Test
    public void testRead() {
        Peripheral uart = new UartPeripheral();
        byte b = uart.read(0);
        // Should not crash, might be 0 if no input
    }
}

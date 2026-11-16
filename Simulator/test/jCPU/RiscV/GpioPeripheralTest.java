package jCPU.RiscV;
import static org.junit.Assert.*;
import org.junit.Test;
public class GpioPeripheralTest {
    @Test public void testOutputReflectedInInput() {
        GpioPeripheral gpio = new GpioPeripheral();
        gpio.write(0, (byte) 0x01);
        gpio.write(4, (byte) 0x01);
        assertEquals((byte) 0x01, gpio.read(8));
    }
    @Test public void testDefaultInputIsZero() {
        GpioPeripheral gpio = new GpioPeripheral();
        assertEquals((byte) 0x00, gpio.read(8));
    }
}

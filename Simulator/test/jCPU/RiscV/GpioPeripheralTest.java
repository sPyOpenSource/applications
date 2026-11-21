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
    @Test
    public void testDirectionReadBack() {
        GpioPeripheral gpio = new GpioPeripheral();
        gpio.write(0, (byte) 0x01);
        assertEquals((byte) 0x01, gpio.read(0));
    }
    @Test
    public void testOutputReadBack() {
        GpioPeripheral gpio = new GpioPeripheral();
        gpio.write(4, (byte) 0x01);
        assertEquals((byte) 0x01, gpio.read(4));
    }
    @Test
    public void testDefaultsAreZero() {
        GpioPeripheral gpio = new GpioPeripheral();
        assertEquals((byte) 0x00, gpio.read(0));
        assertEquals((byte) 0x00, gpio.read(4));
        assertEquals((byte) 0x00, gpio.read(8));
    }
    @Test
    public void testInvalidOffsetReadsZero() {
        GpioPeripheral gpio = new GpioPeripheral();
        assertEquals((byte) 0x00, gpio.read(12));
        assertEquals((byte) 0x00, gpio.read(1));
    }
    @Test
    public void testWriteToInvalidOffsetIsNoOp() {
        GpioPeripheral gpio = new GpioPeripheral();
        gpio.write(12, (byte) 0xFF);
        assertEquals((byte) 0x00, gpio.read(0));
        assertEquals((byte) 0x00, gpio.read(4));
        assertEquals((byte) 0x00, gpio.read(8));
    }
    @Test
    public void testFullByteValue() {
        GpioPeripheral gpio = new GpioPeripheral();
        gpio.write(4, (byte) 0xFF);
        assertEquals((byte) 0xFF, gpio.read(4));
        assertEquals((byte) 0xFF, gpio.read(8));
    }
}

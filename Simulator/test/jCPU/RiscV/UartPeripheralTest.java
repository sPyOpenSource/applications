package jCPU.RiscV;
import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;
public class UartPeripheralTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;
    @Before public void setUp() { testOut = new ByteArrayOutputStream(); System.setOut(new PrintStream(testOut)); }
    @After public void tearDown() { System.setOut(originalOut); }
    @Test public void testWrite() {
        UartPeripheral uart = new UartPeripheral();
        uart.write(0, (byte) 'H');
        uart.write(0, (byte) 'i');
        assertEquals("Hi", testOut.toString());
    }
    @Test public void testRead() throws InterruptedException {
        String input = "Hello";
        UartPeripheral uart = new UartPeripheral(new ByteArrayInputStream(input.getBytes()));
        Thread.sleep(100);
        assertEquals((byte) 'H', uart.read(0));
        assertEquals((byte) 'e', uart.read(0));
        assertEquals((byte) 'l', uart.read(0));
        assertEquals((byte) 'l', uart.read(0));
        assertEquals((byte) 'o', uart.read(0));
        assertEquals((byte) 0, uart.read(0));
    }
}

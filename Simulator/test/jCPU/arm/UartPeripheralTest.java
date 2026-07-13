package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;

public class UartPeripheralTest {
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    private ByteArrayOutputStream testOut;

    @Before
    public void setUp() {
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    @Test
    public void testWrite() {
        Peripheral uart = new UartPeripheral();
        uart.write(0, (byte) 'H');
        uart.write(0, (byte) 'i');
        assertEquals("Hi", testOut.toString());
    }

    @Test
    public void testRead() {
        String input = "Hello";
        Peripheral uart = new UartPeripheral(new ByteArrayInputStream(input.getBytes()));
        
        // Give the reader thread a moment to process the input
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        assertEquals((byte) 'H', uart.read(0));
        assertEquals((byte) 'e', uart.read(0));
        assertEquals((byte) 'l', uart.read(0));
        assertEquals((byte) 'l', uart.read(0));
        assertEquals((byte) 'o', uart.read(0));
        assertEquals((byte) 0, uart.read(0));
    }
}

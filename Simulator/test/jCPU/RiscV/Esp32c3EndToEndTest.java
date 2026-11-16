package jCPU.RiscV;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;

public class Esp32c3EndToEndTest {
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream testOut;

    @Before
    public void setUp() {
        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    // addi x1, x0, 65  ->  0x04100093  (little-endian: 93 00 10 04)
    private static final int ADDI_X1_X0_65 = 0x04100093;
    // addi x1, x0, 66  ->  0x04200093  (little-endian: 93 20 10 04)
    // Second instruction at SRAM_BASE + 4 keeps IsaSim.step()'s
    // code.containsKey(pc) check from early-returning before cc++.
    private static final int ADDI_X1_X0_66 = 0x04200093;

    @Test
    public void testEndToEndExecutesAddi() throws Exception {
        // Two instructions (8 bytes): the first is executed, the second at
        // SRAM_BASE + 4 keeps code.containsKey(SRAM_BASE + 4) true so that
        // IsaSim.step() does not early-exit before incrementing the clock.
        // Image: [0x93,0x00,0x10,0x04, 0x93,0x20,0x10,0x04]
        byte[] image = new byte[8];
        image[0] = (byte) (ADDI_X1_X0_65 & 0xFF);
        image[1] = (byte) ((ADDI_X1_X0_65 >> 8) & 0xFF);
        image[2] = (byte) ((ADDI_X1_X0_65 >> 16) & 0xFF);
        image[3] = (byte) ((ADDI_X1_X0_65 >> 24) & 0xFF);
        image[4] = (byte) (ADDI_X1_X0_66 & 0xFF);
        image[5] = (byte) ((ADDI_X1_X0_66 >> 8) & 0xFF);
        image[6] = (byte) ((ADDI_X1_X0_66 >> 16) & 0xFF);
        image[7] = (byte) ((ADDI_X1_X0_66 >> 24) & 0xFF);

        IsaSim sim = Esp32c3Loader.loadImage(image);
        sim.pc = Esp32c3MemoryMap.SRAM_BASE; // point PC at loaded image

        sim.step();

        assertEquals(65, sim.reg[1]);
        assertEquals(Esp32c3MemoryMap.SRAM_BASE + 4, sim.pc());
        assertEquals(1, sim.getClock());
    }

    @Test
    public void testUartOutputThroughMemoryMap() throws Exception {
        Esp32c3MemoryMap mem = new Esp32c3MemoryMap();
        UartPeripheral uart = new UartPeripheral();
        mem.registerPeripheral(Esp32c3MemoryMap.UART_BASE, 0x1000, uart);

        mem.writeByte(Esp32c3MemoryMap.UART_BASE, (byte) 'H');
        mem.writeByte(Esp32c3MemoryMap.UART_BASE, (byte) 'i');

        assertEquals("Hi", testOut.toString());
    }
}

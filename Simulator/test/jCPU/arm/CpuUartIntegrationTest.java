package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;

public class CpuUartIntegrationTest {
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

    @Test
    public void testWriteByteToUartAddress() throws Exception {
        CPU cpu = new CPU();
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

        vm.write(0x10000000, (int) 'H');

        assertEquals("H", testOut.toString());
    }
}

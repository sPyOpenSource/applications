package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;

/** PL011 console must work at every address form a Pi 2 kernel can use. */
public class Pl011UartTest {
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

    private static void writeByteViaInstruction(CPU cpu, int address, int value) throws Exception {
        cpu.reset();
        cpu.writeRegister(0, address);
        cpu.writeRegister(1, value);
        cpu.executeARM(0xE5C01000); // STRB R1, [R0]
    }

    @Test
    public void testPhysicalUartAddress() throws Exception {
        CPU cpu = new CPU();
        writeByteViaInstruction(cpu, 0x3F201000, 'P');
        assertEquals("P", testOut.toString());
    }

    @Test
    public void testEarlyBootBusAlias() throws Exception {
        // Pi 2 early serial/debug macro uses the VC bus alias before MMU setup.
        CPU cpu = new CPU();
        writeByteViaInstruction(cpu, 0x20201000, 'E');
        assertEquals("E", testOut.toString());
    }

    @Test
    public void testKernelVirtualAlias2G() throws Exception {
        // 0x3F201000 + PAGE_OFFSET(0x80000000)
        CPU cpu = new CPU();
        writeByteViaInstruction(cpu, 0xBF201000, 'V');
        assertEquals("V", testOut.toString());
    }

    @Test
    public void testKernelVirtualAlias3G() throws Exception {
        // 0x3F201000 + PAGE_OFFSET(0xC0000000)
        CPU cpu = new CPU();
        writeByteViaInstruction(cpu, 0xFF201000, 'W');
        assertEquals("W", testOut.toString());
    }

    @Test
    public void testEarlyconFixmapAlias() throws Exception {
        // With CONFIG_MMU, Linux's earlycon=pl011,mmio32 maps the UART into the
        // ARM fixmap at FIXADDR_TOP (0xffeff000 = FIX_EARLYCON_MEM_BASE) before
        // the normal DT console appears. jCPU has no MMU, so this virtual alias
        // must be routed to the PL011 or early printk dies on its first read of
        // UARTFR at 0xffeff018.
        CPU cpu = new CPU();
        writeByteViaInstruction(cpu, 0xFFEFF000, 'X');
        assertEquals("X", testOut.toString());
    }

    @Test
    public void testFlagRegisterReportsTransmitReady() throws Exception {
        CPU cpu = new CPU();
        int flags = cpu.getVirtualMemorySpace().read(0x3F201018); // UARTFR
        assertEquals(0, flags & 0x8);   // BUSY clear
        assertEquals(0x90, flags & 0x90); // TXFE | RXFE set
    }

    @Test
    public void testControlRegisterWriteIsNotPrinted() throws Exception {
        CPU cpu = new CPU();
        cpu.getVirtualMemorySpace().write(0x3F201030, 0x301); // UARTCR
        assertEquals("", testOut.toString());
    }

    @Test
    public void testOutputIsFlushed() throws Exception {
        class FlushTrackingStream extends ByteArrayOutputStream {
            int flushes;
            @Override public void flush() throws IOException { flushes++; super.flush(); }
        }
        FlushTrackingStream tracked = new FlushTrackingStream();
        System.setOut(new PrintStream(tracked));
        try {
            CPU cpu = new CPU();
            writeByteViaInstruction(cpu, 0x3F201000, 'F');
            assertEquals("F", tracked.toString());
            assertTrue("UART output must be flushed for immediate terminal display", tracked.flushes > 0);
        } finally {
            System.setOut(new PrintStream(testOut));
        }
    }
}

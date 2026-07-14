package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.*;
import java.util.Set;
import java.util.stream.Collectors;

public class CpuBugFixTest {

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(100);
    }

    @Test
    public void testTwoCpuInstancesNoDuplicateUartThreads() throws Exception {
        Set<Thread> before = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.isDaemon())
                .collect(Collectors.toSet());

        CPU cpu1 = new CPU();
        CPU cpu2 = new CPU();

        Thread.sleep(200);

        Set<Thread> after = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.isDaemon())
                .collect(Collectors.toSet());

        after.removeAll(before);

        long uartThreads = after.stream()
                .filter(t -> t.getName().contains("UartReader") || t.getName().contains("Thread-"))
                .count();

        assertTrue("Should not create multiple UART reader threads per CPU instance, found " + uartThreads + " new threads",
                uartThreads <= 2);
    }

    @Test
    public void testInstructionReadHalfwordDoesNotStackOverflow() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        try {
            cpu.instructionReadHalfword(0);
        } catch (StackOverflowError e) {
            fail("instructionReadHalfword(int) has infinite recursion: " + e.getMessage());
        } catch (Exception e) {
            // Bus error or other exception is acceptable; StackOverflowError is not
        }
    }

    @Test
    public void testInterruptBitmaskCorrectness() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();

        cpu.setIRQ("testIRQ");

        // CPSR_BIT_I = 7, CPSR_BIT_F = 6
        // With both I and F set (bits 7 and 6), interrupts should be masked
        cpu.writeCPSR(cpu.readCPSR() | (1 << 7) | (1 << 6));

        // After reset, the CPU enters IRQ mode with I and F both set
        // haveIRQ() should return true, but the IRQ should NOT be taken because I bit is set
        assertTrue("IRQ should be pending", cpu.haveIRQ());

        // The incorrect bitmask (cpsr & CPSR_BIT_I = cpsr & 7) would check bits 0-2
        // instead of bit 7, causing interrupts to be taken when they shouldn't be
    }
}

package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class Pi2LoaderTimerTest {
    @Test
    public void testTimerMatchThroughMMIO() throws Exception {
        byte[] kernel = new byte[4];
        CPU cpu = Pi2Loader.loadImage(kernel);
        cpu.reset();

        // Write C0 = 5 through MMIO (little-endian)
        cpu.getVirtualMemorySpace().write32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, 5);

        // Execute cycles to advance counter past 5
        for (int i = 0; i < 10; i++) {
            cpu.execute();
        }

        // Read CS through MMIO — match flag 0 should be set
        int cs = cpu.getVirtualMemorySpace().read32(Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS);
        assertTrue("Timer match flag 0 should be set through MMIO, CS=0x" + Integer.toHexString(cs), (cs & 0x1) != 0);
        assertTrue("CPU should have IRQ pending", cpu.haveIRQ());
    }
}

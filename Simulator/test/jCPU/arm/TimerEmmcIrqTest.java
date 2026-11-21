package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimerEmmcIrqTest {
    @Test
    public void testTimerAndEmmcIrqSharing() throws Exception {
        byte[] kernel = new byte[4];
        java.nio.file.Path sdPath = java.nio.file.Files.createTempFile("timer-emmc-irq-test", ".img");
        java.nio.file.Files.write(sdPath, new byte[512 * 10]);

        try {
            CPU cpu = Pi2Loader.loadImageWithSd(kernel, sdPath.toString());
            cpu.reset();
            VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            // Assert timer IRQ: set C0 = counter + 3, advance past it
            int cloBase = Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CLO;
            int clo = read32(vm, cloBase);
            write32(vm, Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.C0, (int) (clo + 3));
            for (int i = 0; i < 5; i++) {
                cpu.execute();
            }
            read32(vm, cloBase);

            assertTrue("Timer should assert IRQ", cpu.haveIRQ());

            // Assert EMMC IRQ: CMD0 completes and sets INTERRUPT
            write32(vm, Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_GO_IDLE_STATE);

            assertTrue("Both timer and EMMC should assert IRQ", cpu.haveIRQ());

            // Clear timer IRQ only (write-1-to-clear CS bit 0)
            write32(vm, Pi2MemoryMap.SYSTEM_TIMER_BASE + SystemTimer.CS, 0x01);
            assertTrue("EMMC IRQ should still be asserted after clearing timer", cpu.haveIRQ());

            // Clear EMMC IRQ (write-1-to-clear INTERRUPT)
            write32(vm, Pi2MemoryMap.EMMC_BASE + EmmcController.INTERRUPT, EmmcController.IRQ_COMPLETE);
            assertFalse("IRQ should be deasserted after clearing both", cpu.haveIRQ());
        } finally {
            java.nio.file.Files.deleteIfExists(sdPath);
        }
    }

    private int read32(VirtualMemorySpace vm, int addr) {
        return vm.read32(addr);
    }

    private void write32(VirtualMemorySpace vm, int addr, int value) {
        vm.write32(addr, value);
    }
}

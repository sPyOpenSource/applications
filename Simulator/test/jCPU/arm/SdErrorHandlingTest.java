package jCPU.arm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

/**
 * Error-path acceptance tests: out-of-range sectors must set STATUS.ERROR,
 * leave memory/image untouched, and still signal completion.
 */
public class SdErrorHandlingTest {

    private static final int RAM = Pi2MemoryMap.RAM_BASE;

    @Test
    public void testOutOfRangeReadSetsStatusErrorAndLeavesRamUntouched() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[2 * 512]); // sectors 0..1
        try {
            CPU cpu = Pi2Loader.loadImageWithSd(new byte[4], tmp.toString());
            cpu.reset();
            VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            int dst = RAM + 0x4000;
            cpu.instructionWriteWord(dst, 0xDEADBEEF, false);

            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.ARG1, 99, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.DATA, dst, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_READ_SINGLE, false, false);

            int status = vm.readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.STATUS, false, false);
            assertTrue((status & EmmcController.STATUS_ERROR) != 0);
            assertEquals(0xDEADBEEF, cpu.instructionReadWord(dst, false));
            assertTrue(cpu.haveIRQ()); // completion is still signalled so code cannot hang
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testOutOfRangeWriteSetsStatusErrorAndLeavesImageUnchanged() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[2 * 512]);
        try {
            CPU cpu = Pi2Loader.loadImageWithSd(new byte[4], tmp.toString());
            cpu.reset();
            VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.ARG1, 99, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.DATA, RAM + 0x4000, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_WRITE_SINGLE, false, false);

            int status = vm.readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.STATUS, false, false);
            assertTrue((status & EmmcController.STATUS_ERROR) != 0);

            byte[] image = Files.readAllBytes(tmp);
            for (byte b : image) {
                assertEquals(0, b);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}

package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.*;

public class SdEndToEndTest {
    @Test
    public void testWriteThenReadRoundTrip() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[4 * 512]); // 4 sectors so sector 3 exists
        try {
            // Boot through the loader, exactly like the CLI: controller is wired at EMMC_BASE
            CPU cpu = Pi2Loader.loadImageWithSd(new byte[4], tmp.toString());
            cpu.reset();
            VirtualMemorySpace vm = cpu.getVirtualMemorySpace();

            // stage sector 3 in RAM at 0x2000
            byte[] sector = new byte[512];
            for (int i = 0; i < 512; i++) sector[i] = (byte) (i ^ 0x5A);
            for (int i = 0; i < 128; i++) {
                int w = (sector[i*4] & 0xFF) | ((sector[i*4+1] & 0xFF) << 8)
                    | ((sector[i*4+2] & 0xFF) << 16) | ((sector[i*4+3] & 0xFF) << 24);
                cpu.instructionWriteWord(Pi2MemoryMap.RAM_BASE + 0x2000 + i*4, w, false);
            }

            // CMD24: write sector 3 from RAM 0x2000 — all accesses through MMIO, like bare metal
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.ARG1, 3, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x2000, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_WRITE_SINGLE, false, false);
            assertEquals(0, vm.readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.STATUS, false, false) & EmmcController.STATUS_ERROR);
            assertTrue(cpu.haveIRQ());
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.INTERRUPT, EmmcController.IRQ_COMPLETE, false, false);
            assertFalse(cpu.haveIRQ());

            // CMD17: read sector 3 back into RAM 0x3000
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.ARG1, 3, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x3000, false, false);
            vm.writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, EmmcController.CMD_READ_SINGLE, false, false);
            assertEquals(0, vm.readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.STATUS, false, false) & EmmcController.STATUS_ERROR);

            // verify RAM contents match the original sector
            for (int i = 0; i < 128; i++) {
                int w = cpu.instructionReadWord(Pi2MemoryMap.RAM_BASE + 0x3000 + i*4, false);
                byte[] exp = new byte[4];
                exp[0] = (byte) (w & 0xFF); exp[1] = (byte) ((w>>8) & 0xFF);
                exp[2] = (byte) ((w>>16) & 0xFF); exp[3] = (byte) ((w>>24) & 0xFF);
                assertArrayEquals(new byte[]{sector[i*4], sector[i*4+1], sector[i*4+2], sector[i*4+3]}, exp);
            }

            // verify persisted in the host image file
            byte[] img = Files.readAllBytes(tmp);
            assertArrayEquals(sector, java.util.Arrays.copyOfRange(img, 3*512, 3*512+512));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}

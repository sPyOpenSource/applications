package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.*;

public class Pi2LoaderSdTest {
    @Test
    public void testLoaderAcceptsSdImage() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[512]);
        try {
            CPU cpu = Pi2Loader.loadImageWithSd(new byte[] {(byte)0xE3,(byte)0xA0,(byte)0x00,(byte)0x42}, tmp.toString());
            // CMD0 through MMIO must reach the controller registered over the EMMC range
            cpu.getVirtualMemorySpace().writeInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDCMD, 0, false, false);
            int resp = cpu.getVirtualMemorySpace().readInt(Pi2MemoryMap.EMMC_BASE + EmmcController.CMDRESP0, false, false);
            assertEquals(0x01, resp & 0xFF);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}

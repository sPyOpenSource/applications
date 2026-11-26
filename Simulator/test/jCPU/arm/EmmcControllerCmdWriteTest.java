package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.nio.file.*;

public class EmmcControllerCmdWriteTest {
    @Test
    public void testWriteSectorPersistsToImage() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        // NOTE: CPU() already maps 1 MB of RAM at 0x0 — do not map it again.

        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]);
        try (SdCard card = new SdCard(tmp.toString())) {
            EmmcController ctl = new EmmcController(card, cpu);
            cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);

            // fill RAM at 0x1000: little-endian bytes A5 A5 00 00
            for (int i = 0; i < 128; i++) {
                cpu.instructionWriteWord(Pi2MemoryMap.RAM_BASE + 0x1000 + i * 4, 0x0000A5A5, false);
            }
            writeWord(ctl, EmmcController.ARG1, 0);
            writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x1000);
            writeWord(ctl, EmmcController.CMDCMD, 24); // CMD24

            for (int i = 0; i < 10000 && (ctl.getStatus() & EmmcController.STATUS_BUSY) != 0; i++) {}
            assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);

            // verify persisted in image file
            byte[] img = Files.readAllBytes(tmp);
            assertEquals(0xA5, img[0] & 0xFF);
            assertEquals(0xA5, img[1] & 0xFF);
            assertEquals(0x00, img[2] & 0xFF);
            assertEquals(0x00, img[3] & 0xFF);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }
}

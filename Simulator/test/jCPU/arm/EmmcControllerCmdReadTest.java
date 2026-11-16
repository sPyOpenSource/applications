package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerCmdReadTest {
    @Test
    public void testReadSectorDmaToRam() throws Exception {
        CPU cpu = new CPU();
        cpu.reset();
        // NOTE: CPU() already maps 1 MB of RAM at 0x0 — do not map it again.

        SdCard card = tmpImageWithSector(7, 0xDEADBEEF);
        EmmcController ctl = new EmmcController(card, cpu);
        cpu.getVirtualMemorySpace().registerPeripheral(Pi2MemoryMap.EMMC_BASE, 0x1000, ctl);

        // program sector 7 in ARG1
        writeWord(ctl, EmmcController.ARG1, 7);
        // program DATA = RAM destination address
        writeWord(ctl, EmmcController.DATA, Pi2MemoryMap.RAM_BASE + 0x1000);
        // issue CMD17
        writeWord(ctl, EmmcController.CMDCMD, 17);

        // wait until busy clears
        for (int i = 0; i < 10000 && (ctl.getStatus() & EmmcController.STATUS_BUSY) != 0; i++) {}

        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
        // verify first word of sector in RAM
        int val = cpu.instructionReadWord(Pi2MemoryMap.RAM_BASE + 0x1000, false);
        assertEquals(0xDEADBEEF, val);
    }

    private void writeWord(EmmcController ctl, int offset, int value) {
        ctl.write(offset + 0, (byte) (value & 0xFF));
        ctl.write(offset + 1, (byte) ((value >> 8) & 0xFF));
        ctl.write(offset + 2, (byte) ((value >> 16) & 0xFF));
        ctl.write(offset + 3, (byte) ((value >> 24) & 0xFF));
    }

    private SdCard tmpImageWithSector(int sector, int pattern) throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("sd", ".img");
        byte[] img = new byte[(sector + 1) * 512];
        // write the pattern little-endian so the first RAM word reads back as `pattern`
        img[sector * 512 + 0] = (byte) (pattern & 0xFF);
        img[sector * 512 + 1] = (byte) ((pattern >> 8) & 0xFF);
        img[sector * 512 + 2] = (byte) ((pattern >> 16) & 0xFF);
        img[sector * 512 + 3] = (byte) ((pattern >> 24) & 0xFF);
        java.nio.file.Files.write(tmp, img);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { java.nio.file.Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }));
        return new SdCard(tmp.toString());
    }
}

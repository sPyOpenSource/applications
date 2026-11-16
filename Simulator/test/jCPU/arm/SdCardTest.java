package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;

public class SdCardTest {
    @Test
    public void testSectorCount() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]); // 2 sectors
        try (SdCard card = new SdCard(tmp.toString())) {
            assertEquals(2, card.getSectorCount());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testReadWriteRoundTrip() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[1024]);
        try (SdCard card = new SdCard(tmp.toString())) {
            byte[] data = new byte[512];
            for (int i = 0; i < 512; i++) data[i] = (byte) (i * 3 + 1);
            card.writeSector(0, data);
            byte[] back = card.readSector(0);
            assertArrayEquals(data, back);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadOutOfRange() throws Exception {
        Path tmp = Files.createTempFile("sd", ".img");
        Files.write(tmp, new byte[512]);
        try (SdCard card = new SdCard(tmp.toString())) {
            card.readSector(1);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}

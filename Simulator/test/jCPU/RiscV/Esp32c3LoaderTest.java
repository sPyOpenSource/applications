package jCPU.RiscV;

import static org.junit.Assert.*;
import org.junit.Test;

public class Esp32c3LoaderTest {
    @Test
    public void testLoadImagePopulatesSram() throws Exception {
        byte[] image = new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        CPU sim = Esp32c3Loader.loadImage(image);
        assertEquals((byte) 0xDE, sim.readByte(Esp32c3MemoryMap.SRAM_BASE + 0));
        assertEquals((byte) 0xAD, sim.readByte(Esp32c3MemoryMap.SRAM_BASE + 1));
        assertEquals((byte) 0xBE, sim.readByte(Esp32c3MemoryMap.SRAM_BASE + 2));
        assertEquals((byte) 0xEF, sim.readByte(Esp32c3MemoryMap.SRAM_BASE + 3));
    }
}

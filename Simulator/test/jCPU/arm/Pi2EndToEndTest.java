package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class Pi2EndToEndTest {
    @Test
    public void testLoadImageExecutesMov() throws Exception {
        // kernel.img: MOV R0, #0x42 = 0xE3A00042, stored little-endian at 0x00000000
        byte[] image = new byte[4];
        image[0] = (byte) 0x42;
        image[1] = (byte) 0x00;
        image[2] = (byte) 0xA0;
        image[3] = (byte) 0xE3;

        CPU cpu = Pi2Loader.loadImage(image);
        cpu.reset();
        cpu.execute(); // fetch at PC=0, execute MOV, advance PC to 4

        assertEquals(0x42, cpu.r(0));
        assertEquals(4, cpu.readCurrentPC());
    }
}

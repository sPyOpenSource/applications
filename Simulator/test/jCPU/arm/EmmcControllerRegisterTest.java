package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerRegisterTest {
    @Test
    public void testReadUnimplementedReturnsZero() {
        SdCard card = null; // controller constructed without functional card for register test
        EmmcController ctl = new EmmcController(card);
        assertEquals(0, ctl.read(EmmcController.CMDRESP0) & 0xFF);
    }

    @Test
    public void testWriteAndReadArg1() {
        SdCard card = null;
        EmmcController ctl = new EmmcController(card);
        ctl.write(EmmcController.ARG1 + 0, (byte) 0x12);
        ctl.write(EmmcController.ARG1 + 1, (byte) 0x34);
        ctl.write(EmmcController.ARG1 + 2, (byte) 0x56);
        ctl.write(EmmcController.ARG1 + 3, (byte) 0x78);
        int arg = ctl.read(EmmcController.ARG1 + 0) & 0xFF;
        arg |= (ctl.read(EmmcController.ARG1 + 1) & 0xFF) << 8;
        arg |= (ctl.read(EmmcController.ARG1 + 2) & 0xFF) << 16;
        arg |= (ctl.read(EmmcController.ARG1 + 3) & 0xFF) << 24;
        assertEquals(0x78563412, arg);
    }
}

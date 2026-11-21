package jCPU.arm;
import static org.junit.Assert.*;
import org.junit.Test;

public class EmmcControllerCmdTest {
    @Test
    public void testCmd0SetsIdleResponse() {
        EmmcController ctl = new EmmcController(null);
        // write CMDCMD = 0 (CMD0), little-endian 4 bytes
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        // response byte 0 of CMDRESP0 should be 0x01 (idle)
        assertEquals(0x01, ctl.read(EmmcController.CMDRESP0) & 0xFF);
        // busy bit cleared
        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
    }

    @Test
    public void testAcmd41SetsReadyResponse() {
        EmmcController ctl = new EmmcController(null);
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x29); // ACMD41 index
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        assertEquals(0x00, ctl.read(EmmcController.CMDRESP0) & 0xFF);
        assertEquals(0, ctl.getStatus() & EmmcController.STATUS_BUSY);
    }

    @Test
    public void testUnknownCommandSetsError() {
        EmmcController ctl = new EmmcController(null);
        ctl.write(EmmcController.CMDCMD + 0, (byte) 0x30); // CMD48 (unsupported)
        ctl.write(EmmcController.CMDCMD + 1, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 2, (byte) 0x00);
        ctl.write(EmmcController.CMDCMD + 3, (byte) 0x00);
        assertTrue((ctl.getStatus() & EmmcController.STATUS_ERROR) != 0);
    }
}

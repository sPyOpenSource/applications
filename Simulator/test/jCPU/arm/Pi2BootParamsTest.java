package jCPU.arm;

import static org.junit.Assert.*;
import org.junit.Test;

/** Verifies the minimal DTB and boot registers a Pi 2 kernel needs. */
public class Pi2BootParamsTest {
    private static int be32(byte[] b, int off) {
        return ((b[off] & 0xFF) << 24) | ((b[off+1] & 0xFF) << 16)
             | ((b[off+2] & 0xFF) << 8) | (b[off+3] & 0xFF);
    }

    @Test
    public void testDtbHeaderIsValid() {
        byte[] dtb = Pi2BootParams.buildDtb();
        assertTrue("DTB too short", dtb.length > 40);
        assertEquals(0xD00DFEED, be32(dtb, 0));
        assertEquals(dtb.length, be32(dtb, 4));
        assertEquals(40, be32(dtb, 16));       // off_mem_rsvmap
        assertEquals(17, be32(dtb, 20));       // version
        assertTrue("struct block must be nonempty", be32(dtb, 36) > 0);
        assertTrue("strings block must be nonempty", be32(dtb, 32) > 0);
    }

    @Test
    public void testDtbMentionsPi2AndSerialConsole() {
        byte[] dtb = Pi2BootParams.buildDtb();
        String s = new String(dtb, java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(s.contains("brcm,bcm2836"));
        assertTrue(s.contains("arm,pl011"));
        assertTrue(s.contains("console=ttyAMA0"));
        assertTrue(s.contains("bootargs"));
    }

    @Test
    public void testSerialAliasIsStringPath() {
        byte[] dtb = Pi2BootParams.buildDtb();
        String s = new String(dtb, java.nio.charset.StandardCharsets.US_ASCII);
        // DTB aliases are string paths, not phandles. Linux resolves stdout-path
        // "serial0:..." via of_find_node_by_path(), which reads the alias as a
        // NUL-terminated string. A u32 phandle here resolves to an empty path
        // and earlycon silently fails, leaving the console dead.
        assertTrue("serial0 alias must be a full string path",
                   s.contains("/soc/serial@3f201000"));
        assertTrue("stdout-path must reference the serial0 alias",
                   s.contains("serial0:115200n8"));
        assertTrue("PL011 DT binding needs arm,primecell for the AMBA driver",
                   s.contains("arm,primecell"));
    }

    @Test
    public void testDtbIsPlacedWhereZimageDoesNotRelocate() {
        // The zImage decompressor relocates itself to 0x01000000 and overwrites
        // the first words there (observed PC 0x0000017C: stmdb sb!, ...). A DTB
        // placed at 0x01000000 is therefore corrupted before Linux validates
        // it, causing the "no ATAGS support" panic. 0x02000000 is above the
        // relocated image and survives decompression.
        assertEquals(0x02000000, Pi2BootParams.DTB_ADDRESS);
        assertTrue("DTB must not live at the zImage relocation address",
                   Pi2BootParams.DTB_ADDRESS != 0x01000000);
    }

    @Test
    public void testInstallSetsBootRegisters() {
        CPU cpu = Pi2Loader.loadImage(new byte[] {0, 0, 0, 0});
        Pi2BootParams.install(cpu);
        assertEquals(0, cpu.readRegister(0));
        assertEquals(0, cpu.readRegister(1));
        assertEquals(Pi2BootParams.DTB_ADDRESS, cpu.readRegister(2));
    }

    @Test
    public void testInstallCopiesDtbIntoRam() throws Exception {
        CPU cpu = Pi2Loader.loadImage(new byte[] {0, 0, 0, 0});
        Pi2BootParams.install(cpu);
        VirtualMemorySpace vm = cpu.getVirtualMemorySpace();
        assertEquals(0xD00DFEED, vm.readInt(Pi2BootParams.DTB_ADDRESS, false, true));
    }
}

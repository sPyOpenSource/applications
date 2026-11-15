import jCPU.x86.X86VgaPeripheral;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Headless VGA smoke test.
 * Drives the X86VgaPeripheral with a real sequence of text-memory writes
 * (the same path the NasmVM VGA frame displays) and reads them back to
 * verify the VGA state is stored correctly.
 */
public class VgaTest {
    @Test
    public void testVGA() {
        X86VgaPeripheral vga = new X86VgaPeripheral();
        int base = X86VgaPeripheral.VGA_MEM_BASE;

        // --- "real program" sequence: write "Hi!" with attributes to VGA text memory ---
        vga.writeMemory(base + 0, 'H');   // char
        vga.writeMemory(base + 1, 0x0F);  // attribute: white on black
        vga.writeMemory(base + 2, 'i');   // char
        vga.writeMemory(base + 3, 0x0F);  // attribute
        vga.writeMemory(base + 4, '!');   // char
        vga.writeMemory(base + 5, 0x4E);  // attribute: bright red on blue

        // --- read back and verify ---
        boolean ok = true;
        ok &= check(vga, base + 0, 'H', "char H");
        assertTrue("", ok);
        ok &= check(vga, base + 1, 0x0F, "attribute white/black");
        ok &= check(vga, base + 2, 'i', "char i");
        ok &= check(vga, base + 3, 0x0F, "attribute white/black");
        ok &= check(vga, base + 4, '!', "char !");
        ok &= check(vga, base + 5, 0x4E, "attribute red/blue");

        // out-of-range read should return 0
        ok &= check(vga, base - 1, 0, "below range");
        ok &= check(vga, base + 0xFA0, 0, "above range (past text buffer)");

        System.out.println(ok ? "VGA TEST PASSED" : "VGA TEST FAILED");
        System.exit(ok ? 0 : 1);
    }

    static boolean check(X86VgaPeripheral vga, int addr, int expected, String label) {
        int got = vga.readMemory(addr) & 0xFF;
        boolean pass = (got == expected);
        System.out.println((pass ? "PASS" : "FAIL") + " " + label +
                " addr=0x" + Integer.toHexString(addr) +
                " expected=0x" + Integer.toHexString(expected) +
                " got=0x" + Integer.toHexString(got));
        return pass;
    }
}

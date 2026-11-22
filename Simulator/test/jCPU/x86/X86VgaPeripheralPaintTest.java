package jCPU.x86;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Reproduces the blank host VGA display: text written after the component has
 * been painted once never reaches the screen because the backing image is
 * cached too aggressively.
 */
public class X86VgaPeripheralPaintTest
{
    @Test
    public void testPaintReflectsTextWrittenAfterInitialPaint() {
        X86VgaPeripheral vga = new X86VgaPeripheral();
        JComponent display = vga.getDisplay();
        display.setSize(640, 400);

        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();

        // First paint creates the blank backing image, as it does when the
        // GUI window first appears before the guest has written any text.
        vga.paint(g);
        int background = image.getRGB(0, 0);

        // Guest writes 'H' with bright-white foreground into cell (0,0).
        vga.writeMemory(X86VgaPeripheral.VGA_MEM_BASE, (byte) 'H');
        vga.writeMemory(X86VgaPeripheral.VGA_MEM_BASE + 1, (byte) 0x0F);

        vga.paint(g);

        int changed = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 8; x++) {
                if (image.getRGB(x, y) != background) {
                    changed++;
                }
            }
        }
        assertTrue("VGA display stayed blank after text write; changed pixels = " + changed,
                   changed > 0);
        g.dispose();
    }

    @Test
    public void testPaintReflectsAttributeOnlyChangeAfterInitialPaint() {
        X86VgaPeripheral vga = new X86VgaPeripheral();
        JComponent display = vga.getDisplay();
        display.setSize(640, 400);

        BufferedImage image = new BufferedImage(640, 400, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();

        vga.writeMemory(X86VgaPeripheral.VGA_MEM_BASE, (byte) 'H');
        vga.writeMemory(X86VgaPeripheral.VGA_MEM_BASE + 1, (byte) 0x07);
        vga.paint(g);
        int before = image.getRGB(0, 0);

        // Change only the foreground attribute from gray (7) to white (15).
        vga.writeMemory(X86VgaPeripheral.VGA_MEM_BASE + 1, (byte) 0x0F);
        vga.paint(g);

        boolean foundWhite = false;
        for (int y = 0; y < 16 && !foundWhite; y++) {
            for (int x = 0; x < 8 && !foundWhite; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) {
                    foundWhite = true;
                }
            }
        }
        assertTrue("attribute-only write did not refresh the display (was " +
                   Integer.toHexString(before) + ")", foundWhite);
        g.dispose();
    }
}

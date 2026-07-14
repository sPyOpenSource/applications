package j51.swing;

import jCPU.arm.VgaPeripheral;
import java.awt.*;
import javax.swing.*;

public class JVGAConsole extends JComponent {

    private static final int WIDTH = 80, HEIGHT = 25, CHAR_W = 8, CHAR_H = 16;
    private static final int GFX_W = 320, GFX_H = 200;

    private final VgaPeripheral vga;

    public JVGAConsole(VgaPeripheral vga) {
        this.vga = vga;
        setPreferredSize(new Dimension(WIDTH * CHAR_W, HEIGHT * CHAR_H));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH * CHAR_W, HEIGHT * CHAR_H);
    }

    @Override
    public void paint(Graphics g) {
        vga.paint(g);
    }
}
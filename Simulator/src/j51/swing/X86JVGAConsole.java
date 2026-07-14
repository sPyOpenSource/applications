package j51.swing;

import jCPU.x86.X86VgaPeripheral;
import java.awt.*;
import javax.swing.*;

public class X86JVGAConsole extends JComponent {

    private static final int WIDTH = 80, HEIGHT = 25, CHAR_W = 8, CHAR_H = 16;

    private final X86VgaPeripheral vga;

    public X86JVGAConsole(X86VgaPeripheral vga) {
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

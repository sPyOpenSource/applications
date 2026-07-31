package jCPU.x86;

import static jCPU.MCS51.MCS51JVGAConsole.F8x16;
import j51.swing.JVGAConsole;
import j51.swing.VgaPeripheral;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class X86VgaPeripheral extends VgaPeripheral {
    public static final int VGA_MEM_BASE = 0xB8000;
    private static final int VGA_PORT_BASE = 0x3C0;
    private static final int WIDTH = 80, HEIGHT = 25, CHAR_W = 8, CHAR_H = 16;
    private static final int GFX_W = 320, GFX_H = 200;

    private final char[][] txt = new char[HEIGHT][WIDTH];
    private final int[][] colFG = new int[HEIGHT][WIDTH];
    private final int[][] colBG = new int[HEIGHT][WIDTH];
    private final Color[] colTable = new Color[16];

    private final int[] pixels = new int[GFX_W * GFX_H];
    private final int[] palette = new int[256];

    private BufferedImage backImage;
    private BufferedImage gfxImg;
    private final int[] rowBuf = new int[GFX_W];

    private final int mode = 0;
    private final int miscReg = 0;
    private int seqRegIdx = 0;
    private int crtRegIdx = 0;
    private int gfxRegIdx = 0;
    private int attRegIdx = 0;

    private final JVGAConsole display;

    public X86VgaPeripheral() {
        colTable[0] = new Color(0x000000);
        colTable[1] = new Color(0x00007F);
        colTable[2] = new Color(0x007F00);
        colTable[3] = new Color(0x007F7F);
        colTable[4] = new Color(0x7F0000);
        colTable[5] = new Color(0x7F007F);
        colTable[6] = new Color(0x7F7F00);
        colTable[7] = new Color(0x7F7F7F);
        colTable[8] = new Color(0x4F4F4F);
        colTable[9] = new Color(0x0000FF);
        colTable[10] = new Color(0x00FF00);
        colTable[11] = new Color(0x00FFFF);
        colTable[12] = new Color(0xFF0000);
        colTable[13] = new Color(0xFF00FF);
        colTable[14] = new Color(0xFFFF00);
        colTable[15] = new Color(0xFFFFFF);
        for (int i = 0; i < 16; i++)
            palette[i] = colTable[i].getRGB();
        int idx = 16;
        for (int r = 0; r < 6; r++)
            for (int g = 0; g < 6; g++)
                for (int b = 0; b < 6; b++)
                    palette[idx++] = (r * 51) << 16 | (g * 51) << 8 | (b * 51);
        for (int i = 0; i < 24; i++)
            palette[idx++] = (i * 11) << 16 | (i * 11) << 8 | (i * 11);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                txt[y][x] = ' ';
                colFG[y][x] = 7;
            }
        }
        display = new JVGAConsole(this);
    }

    @Override
    public JComponent getDisplay() {
        return display;
    }

    public void writeMemory(int addr, int value) {
        if (addr < VGA_MEM_BASE || addr >= VGA_MEM_BASE + WIDTH * HEIGHT * 2) return;
        int offset = addr - VGA_MEM_BASE;
        int x = (offset / 2) % WIDTH;
        int y = (offset / 2) / WIDTH;
        if ((offset & 1) == 0) {
            txt[y][x] = (char) (value & 0xFF);
        } else {
            colFG[y][x] = value & 0x0F;
            colBG[y][x] = (value >> 4) & 0x0F;
        }
        display.repaint();
    }

    public int readMemory(int addr) {
        if (addr < VGA_MEM_BASE || addr >= VGA_MEM_BASE + WIDTH * HEIGHT * 2) return 0;
        int offset = addr - VGA_MEM_BASE;
        int x = (offset / 2) % WIDTH;
        int y = (offset / 2) / WIDTH;
        if ((offset & 1) == 0) {
            return txt[y][x] & 0xFF;
        } else {
            return (colFG[y][x] & 0x0F) | ((colBG[y][x] & 0x0F) << 4);
        }
    }

    @Override
    public byte read(int port) {
        int offset = port - VGA_PORT_BASE;
        switch (offset) {
            case 0x0A: return (byte) 0; // Input Status 1 (dummy)
            case 0x05: return (byte) 0x20; // Feature Control
            default: return 0;
        }
    }

    @Override
    public void write(int port, byte value) {
        int offset = port - VGA_PORT_BASE;
        switch (offset) {
            case 0x02: attRegIdx = value & 0x1F; break;
            case 0x04: seqRegIdx = value & 0x07; break;
            case 0x0E: crtRegIdx = value & 0x1F; break;
            case 0x0A: gfxRegIdx = value & 0x0F; break;
        }
    }

    @Override
    public void paint(Graphics g) {
        if (mode == 0) {
            Dimension size = display.getSize();
            if (backImage == null || backImage.getWidth() != size.width || backImage.getHeight() != size.height) {
                int w = Math.max(size.width, 1);
                int h = Math.max(size.height, 1);
                backImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                redrawAll(backImage.getGraphics());
            }
            g.drawImage(backImage, 0, 0, display);
        } else {
            if (gfxImg == null)
                gfxImg = new BufferedImage(GFX_W, GFX_H, BufferedImage.TYPE_INT_RGB);
            paintGfx(g);
        }
    }

    private void paintGfx(Graphics g) {
        for (int y = 0; y < GFX_H; y++) {
            int off = y * GFX_W;
            for (int x = 0; x < GFX_W; x++)
                rowBuf[x] = palette[pixels[off + x] & 0xFF];
            gfxImg.setRGB(0, y, GFX_W, 1, rowBuf, 0, 0);
        }
        g.drawImage(gfxImg, 0, 0, display);
    }

    private void redrawAll(Graphics g) {
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                drawChar(g, x, y);
    }

    private void drawChar(Graphics g, int x, int y) {
        int sx = x * CHAR_W, sy = y * CHAR_H, base;
        Color fg = colTable[colFG[y][x]], bg = colTable[colBG[y][x]];
        base = ((int) txt[y][x] & 0xFF) * CHAR_H;
        for (int j = 0; j < CHAR_H; j++) {
            for (int i = 0; i < CHAR_W; i++) {
                g.setColor((F8x16[base] & (1 << (7 - i))) != 0 ? fg : bg);
                g.drawLine(sx + i, sy + j, sx + i, sy + j);
            }
            base++;
        }
    }
}

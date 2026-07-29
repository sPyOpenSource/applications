package jCPU.MCS51;

import j51.swing.JVGAConsole;
import java.awt.*;
import java.awt.image.*;

public class MCS51JVGAConsole extends JVGAConsole implements MCS51Peripheral, XdataWriteListener {

  private static final int BASE = 0xB8000;
  private static final int WIDTH = 80, HEIGHT = 25, CHAR_W = 8, CHAR_H = 16;

  private static final int GFX_BASE = 0xA0000;
  private static final int GFX_W = 320, GFX_H = 200, MODE_REG = 0xBFFFC;

  private final char[][] txt = new char[HEIGHT][WIDTH];
  private final int[][] colFG = new int[HEIGHT][WIDTH];
  private final int[][] colBG = new int[HEIGHT][WIDTH];
  private final Color[] colTable = new Color[16];

  private final int[] pixels = new int[GFX_W * GFX_H];
  private final int[] palette = new int[256];

  private BufferedImage backImage;
  private BufferedImage gfxImg;
  private final int[] rowBuf = new int[GFX_W];

  private int mode = 0;

  public MCS51JVGAConsole() {
    setPreferredSize(new Dimension(WIDTH * CHAR_W, HEIGHT * CHAR_H));
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
  }

  @Override
  public void registerCpu(CPU cpu) {
    cpu.addXdataWriteListener(this);
  }

  @Override
  public boolean xdataWrite(int address, int value) {
    if (address == MODE_REG) {
      mode = value & 1;
      repaint();
      return true;
    }
    if (address >= GFX_BASE && address < GFX_BASE + pixels.length) {
      pixels[address - GFX_BASE] = value & 0xFF;
      repaint();
      return true;
    }
    if (address < BASE || address >= BASE + WIDTH * HEIGHT * 2)
      return false;
    int off = address - BASE;
    int x = (off / 2) % WIDTH;
    int y = (off / 2) / WIDTH;
    if ((address & 1) == 0) {
      txt[y][x] = (char) (value & 0xFF);
    } else {
      colFG[y][x] = value & 0x0F;
      colBG[y][x] = (value >> 4) & 0x0F;
    }
    backImage = null;
    repaint();
    return true;
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(WIDTH * CHAR_W, HEIGHT * CHAR_H);
  }
}

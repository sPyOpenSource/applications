/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm.memory;

import j51.swing.VgaPeripheral;

/**
 * A {@link Memory} that routes accesses to a VGA address range to a
 * {@link VgaPeripheral} while delegating all other accesses to a backing
 * memory. This makes VGA memory-mapped I/O available without touching every
 * memory access call site in the CPU.
 * <p>
 * Note that {@link Memory#read8} and {@link Memory#write8} are private in
 * {@link Memory}, so the {@code read(int)}/{@code write(int,int)} methods are
 * overridden instead; they are the byte-level entry points used by the CPU.
 */
public class VgaMemory extends Memory
{
  // VARIABLES
  private final Memory delegate;
  private final VgaPeripheral vga;
  private final long vgaBase;
  private final int vgaSize;

  // CONSTRUCTORS

  /**
   * Creates a new VgaMemory instance.
   * 
   * @param delegate
   *          the backing memory for all non-VGA addresses;
   * @param vga
   *          the VGA peripheral handling the VGA address range;
   * @param vgaBase
   *          the base address of the VGA memory range;
   * @param vgaSize
   *          the size of the VGA memory range.
   */
  public VgaMemory(Memory delegate, VgaPeripheral vga, long vgaBase, int vgaSize)
  {
    this.delegate = delegate;
    this.vga = vga;
    this.vgaBase = vgaBase;
    this.vgaSize = vgaSize;
  }

  // HELPERS

  private boolean isVga(int aAddr)
  {
    long a = aAddr & 0xFFFFFFFFL;
    return (a >= vgaBase) && (a < vgaBase + vgaSize);
  }

  private int offset(int aAddr)
  {
    return (int) ((aAddr & 0xFFFFFFFFL) - vgaBase);
  }

  // MEMORY ACCESS (VGA range first, then delegate)

  @Override
  public int read32(int aAddr)
  {
    if (isVga(aAddr)) {
      int o = offset(aAddr);
      return (vga.read(o) & 0xFF) << 24 | (vga.read(o + 1) & 0xFF) << 16
          | (vga.read(o + 2) & 0xFF) << 8 | (vga.read(o + 3) & 0xFF);
    }
    return delegate.read32(aAddr);
  }

  @Override
  public short read16(int aAddr)
  {
    if (isVga(aAddr)) {
      int o = offset(aAddr);
      return (short) (((vga.read(o) & 0xFF) << 8) | (vga.read(o + 1) & 0xFF));
    }
    return delegate.read16(aAddr);
  }

  @Override
  public int read(int aAddr)
  {
    if (isVga(aAddr)) {
      return vga.read(offset(aAddr)) & 0xFF;
    }
    return delegate.read(aAddr);
  }

  @Override
  public void write32(int aAddr, int aValue)
  {
    if (isVga(aAddr)) {
      int o = offset(aAddr);
      vga.write(o, (byte) (aValue >>> 24));
      vga.write(o + 1, (byte) (aValue >>> 16));
      vga.write(o + 2, (byte) (aValue >>> 8));
      vga.write(o + 3, (byte) aValue);
      return;
    }
    delegate.write32(aAddr, aValue);
  }

  @Override
  public void write16(int aAddr, short aValue)
  {
    if (isVga(aAddr)) {
      int o = offset(aAddr);
      vga.write(o, (byte) (aValue >>> 8));
      vga.write(o + 1, (byte) aValue);
      return;
    }
    delegate.write16(aAddr, aValue);
  }

  @Override
  public void write(int aAddr, int aValue)
  {
    if (isVga(aAddr)) {
      vga.write(offset(aAddr), (byte) aValue);
      return;
    }
    delegate.write(aAddr, aValue);
  }
}

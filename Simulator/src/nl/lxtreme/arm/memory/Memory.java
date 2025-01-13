/*
 * Java ARM-emu.
 * 
 * (C) Copyright 2011-2012, J.W. Janssen <j.w.janssen@lxtreme.nl>
 */
package nl.lxtreme.arm.memory;


import j51.intel.Code;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import java.util.*;


/**
 * Denotes a memory map, containing various chunks of data.
 */
public class Memory implements Code 
{
  // VARIABLES
  private final List<Chunk> chunks;

  // CONSTRUCTORS

  /**
   * Creates a new Memory instance.
   */
  public Memory()
  {
    this.chunks = new ArrayList<>();
  }

  // METHODS

  /**
   * Creates a new chunk of memory and adds this to this map. If there is
   * already a chunk of memory for the given address range, this method will do
   * nothing.
   * 
   * @param aAddress
   *          the base address of the memory chunk;
   * @param aSize
   *          the size of the memory chunk.
   * @return the (newly created) memory chunk, never <code>null</code>.
   */
  public synchronized Chunk create(long aAddress, int aSize)
  {
    Chunk result = find(aAddress);
    if (result == null)
    {
      result = new Chunk(aAddress, aSize);
      this.chunks.add(result);
    }
    return result;
  }

  /**
   * Finds a chunk of memory that maps the given address.
   * 
   * @param aAddress
   *          the address to return the memory chunk for.
   * @return a memory chunk for the given address, can be <code>null</code> if
   *         no chunk maps to the given address.
   */
  public Chunk find(long aAddress)
  {
    for (Chunk c : this.chunks)
    {
      if (c.maps(aAddress))
      {
        return c;
      }
    }
    return null;
  }

  /**
   * Reads a 16-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 16-bit value at the given memory location.
   */
  @Override
  public short read16(int aAddr)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      return chunk.read16(aAddr);
    } else {
      System.out.printf("Ignoring read from invalid address: 0x%08x\n", aAddr);
      return 0x0;
    }
  }

  /**
   * Reads a 32-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 32-bit value at the given memory location.
   */
  @Override
  public int read32(int aAddr)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      return chunk.read32(aAddr);
    } else {
      System.out.printf("Ignoring read from invalid address: 0x%08x\n", aAddr);
      return 0x00;
    }
  }

  /**
   * Reads a 8-bit value from this memory at the given address.
   * 
   * @param aAddr
   *          the memory location to read.
   * @return the 8-bit value at the given memory location.
   */
  private byte read8(int aAddr)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      return chunk.read8(aAddr);
    } else {
      System.out.printf("Ignoring read from invalid address: 0x%08x\n", aAddr);
      return 0x00;
    }
  }

  /**
   * Writes a 16-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 16-bit value to write.
   */
  @Override
  public void write16(int aAddr, short aValue)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      chunk.write16(aAddr, aValue);
    }
    else
    {
      System.out.printf("Ignoring write to invalid address: 0x%08x\n", aAddr);
    }
  }

  /**
   * Writes a 32-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 32-bit value to write.
   */
  @Override
  public void write32(int aAddr, int aValue)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      chunk.write32(aAddr, aValue);
    } else {
      System.out.printf("Ignoring write to invalid address: 0x%08x\n", aAddr);
    }
  }

  /**
   * Writes a 8-bit value to the memory denoted by the given address.
   * 
   * @param aAddr
   *          the memory location to write;
   * @param aValue
   *          the 8-bit value to write.
   */
  private void write8(int aAddr, byte aValue)
  {
    Chunk chunk = find(aAddr);
    if (chunk != null)
    {
      chunk.write8(aAddr, aValue);
    } else {
      System.out.printf("Ignoring write to invalid address: 0x%08x\n", aAddr);
    }
  }

    @Override
    public boolean getWriteListener() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setWriteListener(boolean mode) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isPresent(int address) {
        Chunk chunk = find(address);
        return chunk != null;
    }

    @Override
    public void setPresent(int from, int to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setSize(int size) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(int addr) {
        return read8(addr);
    }

    @Override
    public int readDirect(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int addr, int value) {
        write8(addr, (byte)value);
    }

    @Override
    public void writeDirect(int addr, int value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addMemoryReadListener(int address, MemoryReadListener l) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addMemoryWriteListener(int address, MemoryWriteListener l) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setCodeSize(int size) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getCodeSize() {
return 0x1000;
        }

    @Override
    public void setCode(int addr, int value) {
        write8(addr, (byte)value);    
    }

    @Override
    public int getCode(int addr, boolean move) {
        return read8(addr);    
    }

    @Override
    public int getCode16(int addr, boolean move) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean containsKey(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int readHalfWord(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void storeHalfWord(int addr, int value) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}

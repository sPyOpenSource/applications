package jCPU.RiscV;

/*
 * Memory implementation for RISC-V Instruction Set Simulator
 * 
 * A simple memory for a single-cycle RISC-V Instruction Set Simulator
 * 
 * @author Hans Jakob Damsgaard (hansjakobdamsgaard@gmail.com)
 */

import j51.intel.Code;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Memory implements Code {
    // A map simulating the memory
    private final Map<Integer, Byte> memory;

    // Constructor for the Memory class
    public Memory() {
        this.memory = new HashMap<>();
    }

    @Override
    public boolean containsKey(int addr) {
        return memory.containsKey(addr);
    }

    @Override
    public int read32(int addr) throws NullPointerException {
        if (memory.containsKey(addr) && memory.containsKey(addr + 1) && 
            memory.containsKey(addr + 2) && memory.containsKey(addr + 3)) {
                return ((memory.get(addr + 3) << 24) & 0xFF000000) | ((memory.get(addr + 2) << 16) & 0x00FF0000) |
                       ((memory.get(addr + 1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF);
        } else {
            throw new NullPointerException();
        }
    }

    @Override
    public short read16(int addr) throws NullPointerException {
        if (memory.containsKey(addr) && memory.containsKey(addr+1)) {
            return (short)(((memory.get(addr + 1) << 8) & 0x0000FF00) | (memory.get(addr) & 0x000000FF));
        } else {
            throw new NullPointerException();
        }
    }

    private int readByte(int addr) throws NullPointerException {
        if (memory.containsKey(addr)) {
            return memory.get(addr) & 0x000000FF;
        } else {
            throw new NullPointerException();
        }
    }

    private void storeWord(int addr, int value) {
        memory.put(addr, (byte) (value));
        memory.put(addr + 1, (byte) (value >> 8));
        memory.put(addr + 2, (byte) (value >> 16));
        memory.put(addr + 3, (byte) (value >> 24));
    }

    @Override
    public void write16(int addr, short value) {
        memory.put(addr, (byte) value);
        memory.put(addr + 1, (byte) (value >> 8));
    }

    private void storeByte(int addr, int value) {
        memory.put(addr, (byte) value);
    }

    public void readBinary(String filePath) throws IOException, EOFException {
            this.readBinary(filePath, 0);
    }

    public void readBinary(String filePath, int initial_pc) throws IOException, EOFException {
        FileInputStream fileStream = null;
        DataInputStream dataStream = null;
        try {
                fileStream = new FileInputStream(filePath);
                dataStream = new DataInputStream(fileStream);
                int localPc = initial_pc, instr;
                while ((instr = dataStream.readInt()) != -1) {
                    storeWord(localPc, Integer.reverseBytes(instr));
                    localPc += 4;
                }
        } catch (IOException e) {
                Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, e);
        } finally {
                if (fileStream != null) {
                        fileStream.close();
                }
                if (dataStream != null) {
                        dataStream.close();
                }
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
        return containsKey(address);
    }

    @Override
    public void setPresent(int from, int to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getSize() {
        return memory.size();
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
        return readByte(addr);
    }

    @Override
    public int readDirect(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int addr, int value) {
        storeByte(addr, value);
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
    public void write32(int aAddr, int aValue) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setCodeSize(int size) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getCodeSize() {
        return getSize();
    }

    @Override
    public void setCode(int addr, int value) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getCode(int addr, boolean move) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public int getCode16(int addr, boolean move) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
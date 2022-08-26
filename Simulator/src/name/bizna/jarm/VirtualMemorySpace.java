package name.bizna.jarm;

import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.iMemory;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VirtualMemorySpace  implements iMemory {
	private final PhysicalMemorySpace mem;
	private final Debugger debugger;
	private int lastAccessAddress, lastAccessWidth;
	private boolean lastAccessWasStore;
	public int getLastAccessAddress() { return lastAccessAddress; }
	public int getLastAccessWidth() { return lastAccessWidth; }
	public boolean getLastAccessWasStore() { return lastAccessWasStore; }
	VirtualMemorySpace(PhysicalMemorySpace mem, Debugger debugger) {
		this.mem = mem;
		this.debugger = debugger;
	}
	public final byte readByte(int address) throws BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onReadMemory(address, 1, false);
		
		lastAccessAddress = address; lastAccessWidth = 0; lastAccessWasStore = false;
		return mem.readByte(address & 0xFFFFFFFFL);
	}
	public final void writeByte(int address, byte value) throws BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onWriteMemory(address, 2, false, value);
		
		lastAccessAddress = address; lastAccessWidth = 0; lastAccessWasStore = true;
		mem.writeByte(address & 0xFFFFFFFFL, value);
	}
	public final short readShort(int address, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onReadMemory(address, 2, bigEndian);
		
		lastAccessAddress = address; lastAccessWidth = 1; lastAccessWasStore = false;
		if((address&1) != 0) {
			if(strictAlign) throw new AlignmentException();
			else if(bigEndian) return (short)((mem.readByte(address&0xFFFFFFFFL)<<8)|(mem.readByte(address+1&0xFFFFFFFFL)&0xFF));
			else return (short)((mem.readByte(address&0xFFFFFFFFL)&0xFF)|(mem.readByte(address+1&0xFFFFFFFFL)<<8));
		}
		else return mem.readShort(address&0xFFFFFFFFL, bigEndian);
	}
	public final void writeShort(int address, short value, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onWriteMemory(address, 2, bigEndian, value);
		
		lastAccessAddress = address; lastAccessWidth = 1; lastAccessWasStore = true;
		if((address&1) != 0) {
			if(strictAlign) throw new AlignmentException();
			else if(bigEndian) {
				mem.writeByte(address&0xFFFFFFFFL, (byte)(value >> 8));
				mem.writeByte(address+1&0xFFFFFFFFL, (byte)value);
			} else {
				mem.writeByte(address&0xFFFFFFFFL, (byte)value);
				mem.writeByte(address+1&0xFFFFFFFFL, (byte)(value >> 8));
			}
		}
		else mem.writeShort(address & 0xFFFFFFFFL, value, bigEndian);
	}
	public final int readInt(int address, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onReadMemory(address, 4, bigEndian);
		
		lastAccessAddress = address; lastAccessWidth = 2; lastAccessWasStore = false;
		if((address&3) != 0) {
			if(strictAlign) throw new AlignmentException();
			else if(bigEndian) return (int)((mem.readByte(address&0xFFFFFFFFL)<<24)|((mem.readByte(address+1&0xFFFFFFFFL)&0xFF)<<16)|((mem.readByte(address+2&0xFFFFFFFFL)&0xFF)<<8)|(mem.readByte(address+3&0xFFFFFFFFL)&0xFF));
			else return (int)((mem.readByte(address&0xFFFFFFFFL)&0xFF)|((mem.readByte(address+1&0xFFFFFFFFL)&0xFF)<<8)|((mem.readByte(address+2&0xFFFFFFFFL)&0xFF)<<16)|(mem.readByte(address+3&0xFFFFFFFFL)<<24));
		}
		else return mem.readInt(address & 0xFFFFFFFFL, bigEndian);
	}
	public final void writeInt(int address, int value, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onWriteMemory(address, 4, bigEndian, value);
		
		lastAccessAddress = address; lastAccessWidth = 2; lastAccessWasStore = true;
		if((address&3) != 0) {
			if(strictAlign) throw new AlignmentException();
			else if(bigEndian) {
				mem.writeByte((address)&0xFFFFFFFFL, (byte)(value >> 24));
				mem.writeByte((address+1)&0xFFFFFFFFL, (byte)(value >> 16));
				mem.writeByte((address+2)&0xFFFFFFFFL, (byte)(value >> 8));
				mem.writeByte((address+3)&0xFFFFFFFFL, (byte)value);
			} else {
				mem.writeByte((address)&0xFFFFFFFFL, (byte)value);
				mem.writeByte((address+1)&0xFFFFFFFFL, (byte)(value >> 8));
				mem.writeByte((address+2)&0xFFFFFFFFL, (byte)(value >> 16));
				mem.writeByte((address+3)&0xFFFFFFFFL, (byte)(value >> 24));
			}
		}
		else mem.writeInt(address & 0xFFFFFFFFL, value, bigEndian);
	}
	public final long readLong(int address, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		int first, second;
		first = readInt(address, strictAlign, bigEndian);
		second = readInt(address+4, strictAlign, bigEndian);
		if(bigEndian) return ((long)first << 32) | (second & 0xFFFFFFFFL);
		else return ((long)second << 32) | (first & 0xFFFFFFFFL);
	}
	public final void writeLong(int address, long value, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(bigEndian) {
			writeInt(address, (int)(value >> 32L), strictAlign, bigEndian);
			writeInt(address+4, (int)value, strictAlign, bigEndian);
		} else {
			writeInt(address, (int)value, strictAlign, bigEndian);
			writeInt(address+4, (int)(value >> 32L), strictAlign, bigEndian);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            try {
                return readByte(addr);
            } catch (BusErrorException | EscapeRetryException ex) {
                Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0;
    }

    @Override
    public int readDirect(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int addr, int value) {
            try {
                writeByte(addr, (byte)value);
            } catch (BusErrorException | EscapeRetryException ex) {
                Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
            }
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
}

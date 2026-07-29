package jCPU.arm;

import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.Peripheral;
import jCPU.iMemory;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VirtualMemorySpace  implements iMemory {
	private final PhysicalMemorySpace mem;
	private final Debugger debugger;
	private final java.util.Map<Integer, Peripheral> mmioMap = new java.util.concurrent.ConcurrentHashMap<>();
	private int lastAccessAddress, lastAccessWidth;
	private boolean lastAccessWasStore;
	public int getLastAccessAddress() { return lastAccessAddress; }
	public int getLastAccessWidth() { return lastAccessWidth; }
	public boolean getLastAccessWasStore() { return lastAccessWasStore; }
	VirtualMemorySpace(PhysicalMemorySpace mem, Debugger debugger) {
		this.mem = mem;
		this.debugger = debugger;
	}
	public void registerPeripheral(int address, Peripheral p) {
		mmioMap.put(address, p);
	}
	public final byte readByte(int address) throws BusErrorException, EscapeRetryException {
		if(debugger != null) debugger.onReadMemory(address, 1, false);
		
		lastAccessAddress = address; lastAccessWidth = 0; lastAccessWasStore = false;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			return p.read(0);
		}
		return mem.readByte(address & 0xFFFFFFFFL);
	}
	public final void writeByte(int address, byte value) throws BusErrorException, EscapeRetryException {
		if(debugger != null) debugger.onWriteMemory(address, 1, false, value);
		
		lastAccessAddress = address; lastAccessWidth = 0; lastAccessWasStore = true;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			p.write(0, value);
			return;
		}
		mem.writeByte(address & 0xFFFFFFFFL, value);
	}
	public final short readShort(int address, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onReadMemory(address, 2, bigEndian);
		
		lastAccessAddress = address; lastAccessWidth = 1; lastAccessWasStore = false;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			byte b0 = p.read(0);
			byte b1 = p.read(1);
			if (bigEndian) return (short)(((b0 & 0xFF) << 8) | (b1 & 0xFF));
			else return (short)((b0 & 0xFF) | ((b1 & 0xFF) << 8));
		}
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
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			if (bigEndian) {
				p.write(0, (byte)(value >> 8));
				p.write(1, (byte)value);
			} else {
				p.write(0, (byte)value);
				p.write(1, (byte)(value >> 8));
			}
			return;
		}
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
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			byte b0 = p.read(0);
			byte b1 = p.read(1);
			byte b2 = p.read(2);
			byte b3 = p.read(3);
			if (bigEndian) return (int)(((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF));
			else return (int)((b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24));
		}
		if((address&3) != 0) {
			if(strictAlign) throw new AlignmentException();
			else if(bigEndian) return (int)(((mem.readByte(address&0xFFFFFFFFL)&0xFF)<<24)|((mem.readByte(address+1&0xFFFFFFFFL)&0xFF)<<16)|((mem.readByte(address+2&0xFFFFFFFFL)&0xFF)<<8)|(mem.readByte(address+3&0xFFFFFFFFL)&0xFF));
			else return (int)((mem.readByte(address&0xFFFFFFFFL)&0xFF)|((mem.readByte(address+1&0xFFFFFFFFL)&0xFF)<<8)|((mem.readByte(address+2&0xFFFFFFFFL)&0xFF)<<16)|((mem.readByte(address+3&0xFFFFFFFFL)&0xFF)<<24));
		}
		else return mem.readInt(address & 0xFFFFFFFFL, bigEndian);
	}
	public final void writeInt(int address, int value, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onWriteMemory(address, 4, bigEndian, value);
		
		lastAccessAddress = address; lastAccessWidth = 2; lastAccessWasStore = true;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			if (bigEndian) {
				p.write(0, (byte)(value >> 24));
				p.write(1, (byte)(value >> 16));
				p.write(2, (byte)(value >> 8));
				p.write(3, (byte)value);
			} else {
				p.write(0, (byte)value);
				p.write(1, (byte)(value >> 8));
				p.write(2, (byte)(value >> 16));
				p.write(3, (byte)(value >> 24));
			}
			return;
		}
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
		if(debugger!=null) debugger.onReadMemory(address, 8, bigEndian);
		
		lastAccessAddress = address; lastAccessWidth = 3; lastAccessWasStore = false;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			long result = 0;
			for (int i = 0; i < 8; i++) {
				long b = p.read(i) & 0xFFL;
				if (bigEndian) result = (result << 8) | b;
				else result |= (b << (i * 8));
			}
			return result;
		}
		int first, second;
		first = readInt(address, strictAlign, bigEndian);
		second = readInt(address+4, strictAlign, bigEndian);
		lastAccessAddress = address; lastAccessWidth = 3; lastAccessWasStore = false;
		if(bigEndian) return ((long)first << 32) | (second & 0xFFFFFFFFL);
		else return ((long)second << 32) | (first & 0xFFFFFFFFL);
	}
	public final void writeLong(int address, long value, boolean strictAlign, boolean bigEndian) throws AlignmentException, BusErrorException, EscapeRetryException {
		if(debugger!=null) debugger.onWriteMemory(address, 8, bigEndian, value);
		
		lastAccessAddress = address; lastAccessWidth = 3; lastAccessWasStore = true;
		Peripheral p = mmioMap.get(address);
		if (p != null) {
			for (int i = 0; i < 8; i++) {
				if (bigEndian) p.write(i, (byte)(value >> (56 - i * 8)));
				else p.write(i, (byte)(value >> (i * 8)));
			}
			return;
		}
		if(bigEndian) {
			writeInt(address, (int)(value >> 32L), strictAlign, bigEndian);
			writeInt(address+4, (int)value, strictAlign, bigEndian);
		} else {
			writeInt(address, (int)value, strictAlign, bigEndian);
			writeInt(address+4, (int)(value >> 32L), strictAlign, bigEndian);
		}
		lastAccessAddress = address; lastAccessWidth = 3; lastAccessWasStore = true;
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

    @Override
    public int read32(int aAddr) {
        try {
            return readInt(aAddr, false, false);
        } catch (AlignmentException | BusErrorException | EscapeRetryException ex) {
            Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    @Override
    public void write32(int aAddr, int aValue) {
        try {
            writeInt(aAddr, aValue, false, false);
        } catch (AlignmentException | BusErrorException | EscapeRetryException ex) {
            Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void write16(int aAddr, short aValue) {
        try {
            writeShort(aAddr, aValue, false, false);
        } catch (AlignmentException | BusErrorException | EscapeRetryException ex) {
            Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public short read16(int aAddr) {
        try {
            return readShort(aAddr, false, false);
        } catch (AlignmentException | BusErrorException | EscapeRetryException ex) {
            Logger.getLogger(VirtualMemorySpace.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }

    @Override
    public boolean containsKey(int addr) {
        return mmioMap.containsKey(addr);
    }
}

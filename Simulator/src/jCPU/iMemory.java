/**
 * $Id: Memory.java 62 2010-06-29 22:06:12Z mviara $
 */
package jCPU;


/**
 * Memory interface.
 * 
 * @author Mario Viara
 * @version 1.00
 * 
 * @since 1.04
 */
public interface iMemory
{
	public boolean getWriteListener();
	public void setWriteListener(boolean mode);
	public boolean isPresent(int address);
	public void setPresent(int from, int to);
	public int getSize();
	public String getName();
	public void setSize(int size);
	public int read(int addr);
	public int readDirect(int addr);
	public void write(int addr, int value);
	public void writeDirect(int addr, int value);
	public void addMemoryReadListener(int address, MemoryReadListener l);
	public void addMemoryWriteListener(int address, MemoryWriteListener l);
        public int read32(int aAddr);
        public void write32(int aAddr, int aValue);
        public void write16(int aAddr, short aValue);
        public short read16(int aAddr);
        public boolean containsKey(int addr);
        public int readHalfWord(int addr);
        public void storeHalfWord(int addr, int value);
}

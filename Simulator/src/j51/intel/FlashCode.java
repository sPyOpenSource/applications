/**
 * $Id: FlashCode.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

/**
 *
 * Implementation of code in flash memory.
 * 
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	New Version using PersistentMemory
 */
public class FlashCode extends PersistentMemory implements Code
{
	
	public FlashCode(String name,int size)
	{
		super(name, "flash", size);
	}

        @Override
	public void setCodeSize(int size)
	{
		setSize(size);
	}

        @Override
	public int getCodeSize()
	{
		return getSize();
	}

        @Override
	public int getCode(int addr, boolean fetch)
	{
		return read(addr);
	}

        @Override
	public int getCode16(int addr, boolean fetch)
	{
		return ((getCode(addr, fetch) & 0xff) << 8) | (getCode(addr + 1, fetch) & 0xff);
	}
	
        @Override
	public void setCode(int addr, int value)
	{
		write(addr, value);
	}

    @Override
    public int read32(int aAddr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void write32(int aAddr, int aValue) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void write16(int aAddr, short aValue) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public short read16(int aAddr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean containsKey(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}

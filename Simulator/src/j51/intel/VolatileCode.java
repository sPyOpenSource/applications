/**
 * $Id: VolatileCode.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

import j51.device.VolatileMemory;

/**
 *
 * Volatile interface for code interface.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	New version based on volatile memory.
 */
public class VolatileCode extends VolatileMemory implements Code
{
	
        @Override
	public void setCodeSize(int size)
	{
		setSize(size);
	}
	
        @Override
	public int  getCodeSize()
	{
		return getSize();
	}
	
        @Override
	public void setCode(int addr,int value)
	{
		write(addr, value);
	}
	
        @Override
	public int  getCode(int addr, boolean fetch)
	{
		return read(addr);
	}

        @Override
	public int  getCode16(int addr, boolean fetch)
	{
		return ((read(addr) & 0xff) << 8 ) | (read(addr+1) & 0xff);
	}

    @Override
    public int read32(int addr) {
      int b1 = read(addr + 0) & 0xff;
      int b2 = read(addr + 1) & 0xff;
      int b3 = read(addr + 2) & 0xff;
      int b4 = read(addr + 3) & 0xff;
      return ( b1 << 24 ) | ( b2 << 16 ) | ( b3 << 8 ) | b4;    }

    @Override
    public void write32(int addr, int aValue) {
      write(addr + 0, ( byte )( ( aValue >> 24 ) & 0xff ));
      write(addr + 1, ( byte )( ( aValue >> 16 ) & 0xff ));
      write(addr + 2, ( byte )( ( aValue >> 8 ) & 0xff ));
      write(addr + 3, ( byte )( aValue & 0xff ));
    }

    @Override
    public boolean containsKey(int addr) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}

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
class VolatileCode extends VolatileMemory implements Code
{
	
	public void setCodeSize(int size)
	{
		setSize(size);
	}
	
	public int  getCodeSize()
	{
		return getSize();
	}
	
	public void setCode(int addr,int value)
	{
		write(addr,value);
	}
	
	public int  getCode(int addr,boolean fetch)
	{
		return read(addr);
	}

	public int  getCode16(int addr,boolean fetch)
	{
		return ((read(addr) & 0xff) << 8 ) | (read(addr+1) & 0xff);
	}
}

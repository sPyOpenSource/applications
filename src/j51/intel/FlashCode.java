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
		super(name,"flash",size);
	}

	public void setCodeSize(int size)
	{
		setSize(size);
	}

	public int getCodeSize()
	{
		return getSize();
	}

	public int getCode(int addr,boolean fetch)
	{
		return read(addr);
	}

	public int getCode16(int addr,boolean fetch)
	{
		return ((getCode(addr,fetch) & 0xff) << 8) | (getCode(addr+1,fetch) & 0xff);
	}
	
	public void setCode(int addr,int value)
	{
		write(addr,value);
	}

}

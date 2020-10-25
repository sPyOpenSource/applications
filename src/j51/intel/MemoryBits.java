/**
 * $Id: MemoryBits.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

/**
 * Class to rappresent a pool of bit in memory.
 *
 * @author Mario Viara
 * @version 1.00
 * 
 * @since 1.04
 */
public class MemoryBits 
{
	protected int mask;
	protected int shift;
	private int width;
	private Memory memory;
	private int address;

	public MemoryBits(Memory memory,int address,int shift,int width)
	{
		this.width = width;
		this.mask = (1 << width) - 1;
		this.shift = shift;
		this.address = address;
		this.memory = memory;
	}


	public int getWidth()
	{
		return width;
	}



	public int getBits() 
	{
		int value = memory.read(address);
		value >>= shift;
		value &= mask;
		return value;
	}

	public void setBits(int v) 
	{
		synchronized(memory)
		{
			int value = memory.read(address);
			v &= mask;
			value &= ~(mask << shift);
			value |= v << shift;
			memory.write(address,value);
		}
	}

	public void addMemoryWriteListener(MemoryWriteListener l)
	{
		memory.addMemoryWriteListener(address,l);
	}

}


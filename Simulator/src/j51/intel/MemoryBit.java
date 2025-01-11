/**
 * $Id: MemoryBit.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

import jCPU.iMemory;

/**
 * Class to rappresent a single bit of memory.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public class MemoryBit extends MemoryBits
{

	public MemoryBit(iMemory memory,int a,int b)
	{
		super(memory,a,b,1);
	}


	public boolean get() 
	{
		return getBits() != 0;
	}

	public void set(boolean v) 
	{
		setBits(v == true ? 1 : 0);
	}


}


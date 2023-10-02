/**
 * $Id: OpenCollectorMemoryBit.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

import j51.util.*;
import jCPU.MemoryReadListener;
import jCPU.iMemory;

/**
 * 
 * Memory bit shared between one cpu and one peripheral. This bit
 * operate as a 'Open Collector' circuit if the peripheral set the bit
 * to 0 the result will be 0 otherwise the result will be read from the
 * associated memory.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public class OpenCollectorMemoryBit extends MemoryBit   implements MemoryReadListener
{
	private static Logger log = Logger.getLogger(OpenCollectorMemoryBit.class);
	private boolean bit = true;

	
	public OpenCollectorMemoryBit(iMemory memory,int a,int b)
	{
		super(memory, a, b);

		memory.addMemoryReadListener(a, this);
	}


	public boolean getLocal()
	{
		return bit;
	}
	
	public boolean get()
	{
		return bit & super.get();
	}

	public void set(boolean v)
	{
		bit = v;
	}

	public int readMemory(int a,int v)
	{
		int value = v;
		
		if (!bit)
			value &=   ~(mask << shift);

		
		return value;
	}

	public String toString()
	{
		return "OCBit "+bit+" super "+super.get();
	}
}

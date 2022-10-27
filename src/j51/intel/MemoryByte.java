/*
 * $Id: MemoryByte.java 75 2010-07-07 06:04:05Z mviara $
 */
package j51.intel;

import j51.util.FastArray;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;

/**
 * Memory byte used from VolatileMemory implementation. For performance
 * reason access to the field of this class are direct and no method is
 * provided for set/get value,flag and so on.
 * 
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.05
 *
 * 1.01	Member mw and mr are now initialized to null, and created when
 *	necessary.
 */
public class MemoryByte
{
	public int value = 0;
	public String name = "";
	public FastArray<MemoryWriteListener> mw = null;
	public FastArray<MemoryReadListener> mr = null;
	public boolean readBusy = false;
	public boolean writeBusy = false;
	public boolean present = false;

	
	public void setName(String name)
	{
		this.name = name;
	}


	public String getName()
	{
		return name;
	}

}

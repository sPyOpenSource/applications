/**
 * $Id: VolatileMemory.java 75 2010-07-07 06:04:05Z mviara $
 */
package j51.intel;


import j51.util.Hex;
import j51.util.Logger;
import j51.util.FastArray;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.iMemory;
import java.util.logging.Level;

/**
 * Volatile implementation of memory based over one array of MemoryByte.
 * 
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01 New version with MemoryByte for better performance.
 * 	Most used method now are final for better performance.
 * 
 * @since 1.04
 */
public class VolatileMemory implements iMemory
{
	private static Logger log = Logger.getLogger(VolatileMemory.class);
	protected String name = "";
	protected MemoryByte memory[] = new MemoryByte[0];
	private boolean writeListener;
	private int size = 0;
	
	public VolatileMemory()
	{
		this(0);
	}
	
	public VolatileMemory(int size)
	{
		this("Memory", size);
	}

	public VolatileMemory(String name, int size)
	{
		this(name, "memory", size);
	}

	public VolatileMemory(String name,String suffix,int size)
	{
		this.name = name+"."+size+"."+suffix;

		/**
		 * Defaul memory limit is 64K because the memory are
		 * used in a 16 bit address processor.
		 */
		setLimit(0x10000);
		setSize(size);

		writeListener = true;
	}

	public boolean getWriteListener()
	{
		return writeListener;
	}
	
	public void setWriteListener(boolean mode)
	{
		writeListener = mode;
	}
	
	public String getName()
	{
		return name;
	}

	private void setLimit(int limit)
	{
		if (limit < memory.length)
			return;
		
		memory  = new MemoryByte[limit];

		for (int i = memory.length ; --i >= 0;)
			memory[i] = new MemoryByte();
	}
	
	public  void setSize(int size)
	{
		setLimit(size);
		if (this.size != size)
		{
			setPresent(0,size);
			log.fine(name+" Resize from "+this.size+" to "+size);
			this.size = size;
		}
	}

	public final int getSize()
	{
		return size;
	}


	public void setPresent(int from,int start)
	{
		for (int i = from ; i < start ; i++)
		     memory[i].present = true;
	}
	
	public boolean isPresent(int address)
	{
		return memory[address].present;
	}

	/**
	 * Check if one address is present. If the address is not
	 * present one java.lang.Error is throw with the specified
	 * message and the address.
	 *
	 * @param address - Address to check.
	 * @param msg - Message error.
	 *
	 * @author Mario Viara
	 * @version 1.00
	 * @since 1.05
	 */
	private final void checkAddress(int address,String msg)
	{
		if (!isPresent(address))
			throw new java.lang.Error(name+msg+" AT 0x"+Hex.bin2word(address)+" out of range, Max 0x"+Hex.bin2word(size));
	}

	public final int readDirect(int address)
	{
		checkAddress(address,"Read direct");

		return memory[address].value & 0xff;
	}

	public final int read(int address)
	{
		checkAddress(address,"Read");
		MemoryByte b = memory[address];
		int value = b.value;

		if (b.mr != null)
		{
			if (!b.readBusy)
			{
				b.readBusy = true;

				for (int i =  b.mr.size(); --i >= 0; )
					value = b.mr.get(i).readMemory(address,value);

				b.readBusy = false;
			} else {
				log.log(Level.FINE, "Read busy at {0} Memory {1}", new Object[]{Hex.bin2word(address), this});

			}
		}
		
		return value & 0xff;
	}

	public final void writeDirect(int address,int newValue)
	{
		checkAddress(address,"Write direct");

		memory[address].value = newValue;

	}
	
	public void write(int address,int newValue)
	{
		checkAddress(address,"Write");
		MemoryByte b = memory[address];
		
		int oldValue = b.value;
		b.value = newValue;
		
		if (writeListener && b.mw != null)
		{
			if (!b.writeBusy)
			{
				b.writeBusy = true;

				
				for (int i = b.mw.size(); --i >= 0 ; ){
					b.mw.get(i).writeMemory(address, newValue, oldValue);
                                }


				b.writeBusy = false;
			} else {
				log.log(Level.FINE, "Write busy at {0} Memory {1}", new Object[]{Hex.bin2word(address), this});
			}


		}

	}

        @Override
	public void addMemoryReadListener(int address,MemoryReadListener l)
	{
		MemoryByte b = memory[address];
		
		b.present = true;
		if (b.mr == null)
			b.mr = new FastArray<>();

		b.mr.add(l);
	}
	
        @Override
	public void addMemoryWriteListener(int address,MemoryWriteListener l)
	{
		MemoryByte b = memory[address];
		b.present = true;
		
		if (b.mw == null)
			b.mw = new FastArray<>();

		b.mw.add(l);
	}

	/**
	 * Set the name for a byte of memory
	 *
	 * @param i - Address.
	 * @param s - New name.
	 * 
	 * @since 1.05
	 */
	public void setName(int i,String s)
	{
		memory[i].setName(s);
	}

	/**
	 * Return the name of a memory byte.
	 *
	 * @param i - Address.
	 *
	 * @since 1.05
	 */
	public String getName(int i)
	{
		return memory[i].getName();
	}
	
	/**
	 * Return the memory byte at specified address.
	 *
	 * @param a - address
	 *
	 * @since 1.05
	 */
	protected MemoryByte getMemory(int a)
	{
		return memory[a];
	}

	/**
	 * Set a memory byte.
	 *
	 * @param a - Address.
	 * @param b - Memory byte.
	 *
	 * @since 1.05
	 */
	protected void setMemory(int a,MemoryByte b)
	{
		memory[a] = b;
	}
	
	public String toString()
	{
		return name;
	}

	
}

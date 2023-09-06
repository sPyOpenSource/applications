/**
 * $Id: PersistentBuffer.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

import java.io.*;
import javax.swing.Timer;
import j51.util.Hex;

/**
 *
 * Class to implements persistent data like eeprom or flash in u.
 *
 * @author Mario Viara
 * @version 1.00
 * @deprecated Use PersistentMemory
 * 
 */
public class PersistentBuffer 
{
	private byte buffer[];
	private String filename;
	private boolean loaded = false;
	private Timer timer = null;
	private String name;
	
	public PersistentBuffer(String name,String suffix,int size)
	{
		buffer = new byte[size];
		this.name = name;
		filename = name + "." + size + "." + suffix;

		for (int i = 0 ; i < size ; i++)
			buffer[i] = (byte)0xff;

		try
		{
			FileInputStream is = new FileInputStream(new File(filename));
			is.read(buffer);
			is.close();
			loaded  = true;
			System.out.println(filename+" loaded");
		}
		catch (IOException ex)
		{
		}
	}

	public boolean isLoaded()
	{
		return loaded;
	}
	
	public int getSize()
	{
		return buffer.length;
	}

	private void checkAddr(int addr)
	{
		if (addr >= buffer.length)
		{
			throw new java.lang.Error(name + " 0x" + Hex.bin2word(addr) + " out of range, max 0x" + Hex.bin2word(buffer.length));
		}
		
	}
	
	public int get(int addr)
	{
		checkAddr(addr);
		return buffer[addr] & 0xff;
	}

	public int get16(int addr)
	{
		return ((buffer[addr] & 0xff) << 8 ) | (buffer[addr + 1] & 0xff);
		
	}
	
	public synchronized void set(int addr,int value)
	{
		byte b = (byte)(value & 0xff);

		checkAddr(addr);
		
		if (buffer[addr] != b)
		{
			buffer[addr] = b;
			if (timer == null)
			{
				timer = new javax.swing.Timer(1000,new java.awt.event.ActionListener()
				{
                                        @Override
					public void actionPerformed(java.awt.event.ActionEvent e)
					{
						timer.stop();
						save();
					}
				});
				timer.start();
			}
			else
				timer.restart();
						
		}
	}

	private synchronized void save()
	{
		try
		{
			FileOutputStream os = new FileOutputStream(new File(filename));
			os.write(buffer);
			os.close();
			System.out.println(filename+" saved");
		}
		catch (IOException ex)
		{
			System.out.println(ex);
		}
	}
}

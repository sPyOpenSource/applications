/**
 * $Id: PersistentMemory.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.intel;

import java.io.*;
import javax.swing.Timer;

import j51.util.Logger;

/**
 * Persistent implementation of memory.
 * 
 * @author Mario Viara
 * @version 1.00
 * 
 * @since 1.04
 */
public class PersistentMemory extends VolatileMemory
{
	private static Logger log = Logger.getLogger(PersistentMemory.class);
	private Timer timer = null;
	private boolean loaded = false;
	
	public PersistentMemory(String name,String suffix,int size)
	{
		super(name,suffix,size);
		load();
	}

	public void write(int address,int value)
	{
		byte b = (byte)(value & 0xff);
		

		if (readDirect(address) != value)
		{
			if (timer == null)
			{
				timer = new javax.swing.Timer(1000,new java.awt.event.ActionListener()
				{
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

		super.write(address,value);
	
	}
	
	private synchronized void save()
	{
		try
		{
			FileOutputStream os = new FileOutputStream(new File(getName()));
			for (int i = 0 ; i < getSize() ; i++)
			{
				byte b = (byte)readDirect(i);
				os.write(b);
			}
			
			os.close();
			log.info(getName()+" saved "+getSize()+" bytes");
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}
	}

	public boolean isLoaded()
	{
		return loaded;
	}
	
	private void load()
	{

		try
		{
			FileInputStream is = new FileInputStream(new File(getName()));
			for (int i = 0 ; i < getSize() ; i++)
			{
				byte b[] = new byte[1];
				is.read(b);
				writeDirect(i,b[0]);
			}
			is.close();
			log.info(getName()+" loaded "+getSize()+" bytes");
			loaded = true;
		}
		catch (Exception ex)
		{
		}
	}

	public void setSize(int size)
	{
		if (getSize() != size)
		{
			super.setSize(size);
			load();
		}
			
	}

	
}

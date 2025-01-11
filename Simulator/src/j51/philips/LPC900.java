/**
 * $Id: LPC900.java 74 2010-07-07 05:50:03Z mviara $
 */
package j51.philips;

import j51.intel.*;
import j51.swing.JUart;
import j51.util.Logger;
import jCPU.MemoryReadListener;
import jCPU.MemoryWriteListener;
import jCPU.iMemory;


/**
 *
 * LPC900 Family emulator.
 * 
 * Emulated peripheral :
 *
 * - Flash memory
 * - Eeprom
 * - Double DPTR
 * - Watch Dog
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01 Added support for software reset using AUXR1_SRST.
 *	Added constructor with code size.
 * 
 */
public class LPC900 extends MCS51 implements SfrWriteListener, LPC900Constants,
			MemoryReadListener, MemoryWriteListener
{
	private static Logger log = Logger.getLogger(LPC900.class);
	private byte flashBuffer[];
	private int  flashCounter;
	private iMemory eeprom;
	private FlashCode flash;
	private boolean eepromWrite = false;
	private PersistentMemory misc;
	

	public LPC900() throws Exception
	{
		this("LPC900");
	}

	public LPC900(String name) throws Exception
	{
		this(name,8192);
	}

	/**
	 * Complete constructor.
	 *
	 * @param name - Name of the processor.
	 * @param flashSize - Flash size in bytes
	 *
	 * @version 1.00
	 * @since 1.01
	 */
	public LPC900(String name,int flashSize) throws Exception
	{
		super(7372800);
		machineCycle = 2;
		
		eeprom  = new PersistentMemory(name,"eeprom",	512);
		misc	= new PersistentMemory(name,"misc",	32);

		flash	= new FlashCode(name,	flashSize);
		flashBuffer = new byte[64];

		setCode(flash);
		
		// XDATA is always 512 byte
		setXdataSize(512);

		for (int i = 0 ; i < 16 ; i++)
		{
			flash.addMemoryReadListener(FLASH_MISC + i, this);
			flash.addMemoryWriteListener(FLASH_MISC + i, this);
		}
		
		if (misc.isLoaded() == false)
		{
			misc.write(UCFG1,0x63);
			misc.write(BOOTV,0x1F);
			misc.write(BOOTSTAT,0x01);
			
			for (int i = 0 ; i < 8 ; i++)
				misc.write(SEC0+i,0);
		}
		

		
		addPeripheral(new j51.device.Timer());
		addPeripheral(new LPC900Misc());
		addPeripheral(new LPC900Ports());
		addPeripheral(new JUart());
		addPeripheral(new LPC900WDT());
		
		/**
		 * Flash interface
		 */
		addSfrWriteListener(LPC900Constants.FMCON,this);
		addSfrWriteListener(LPC900Constants.FMDATA,this);
		addSfrWriteListener(LPC900Constants.DEEDAT,this);
		addSfrWriteListener(LPC900Constants.DEEADR,this);
		addSfrWriteListener(LPC900Constants.AUXR1,this);



		setSfrName(LPC900Constants.FMDATA,	"FMDATA");
		setSfrName(LPC900Constants.FMCON,	"FMCON");
		setSfrName(LPC900Constants.FMADRL,	"FMADRL");
		setSfrName(LPC900Constants.FMADRH,	"FMADRH");
		setSfrName(LPC900Constants.DEECON,	"DEECON");
		setSfrName(LPC900Constants.DEEDAT,	"DEEDAT");
		setSfrName(LPC900Constants.DEEADR,	"DEEADR");
		setSfrName(LPC900Constants.AUXR1,	"AUXR1");
	}

	


	public void writeMemory(int address,int newValue,int oldValue)
	{
		flash.write(address - FLASH_MISC,newValue);
	}

	public int readMemory(int address,int value)
	{
		return flash.read(address - FLASH_MISC);

	}


	
	int miscRead(int add)
	{
		return misc.read(add);
	}

	void miscWrite(int add,int value)
	{
		misc.write(add,value);
	}

	public void reset()
	{
		super.reset();
		
		if ((miscRead(BOOTSTAT) & 0x01) != 0)
		{
			pc(miscRead(BOOTV) << 8);
		}

		sfrWrite(FMCON,0x70);
	}
	
	
	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	LPC900Constants.AUXR1:
				
				// Bit 1 is always 0
				v &= ~AUXR1_0;
				sfr(LPC900Constants.AUXR1,v);
				swapDptr(v & AUXR1_DPS);
				if ((v & LPC900Constants.AUXR1_SRST) != 0)
					reset();
				break;
				
			case	LPC900Constants.DEEDAT:
				eepromWrite = true;
				break;
				
			case	LPC900Constants.DEEADR:
				int addr = v + (sfr(LPC900Constants.DEECON) & 1) * 256;
				if (eepromWrite)
				{
					eeprom.write(addr,sfr(LPC900Constants.DEEDAT));
				}
				else
				{
					sfr(LPC900Constants.DEEDAT,eeprom.read(addr));
				}
				sfr(LPC900Constants.DEECON,sfr(LPC900Constants.DEECON)|0x80);
				eepromWrite = false;
				break;
				
			case	LPC900Constants.FMDATA:
				flashBuffer[flashCounter++] = (byte)v;
				break;
				
			case	LPC900Constants.FMCON:
				switch (v)
				{
					case	0x00:
						flashCounter = 0;
						break;
					case	0x68:
						int base = sfr(LPC900Constants.FMADRL) +
							   sfr(LPC900Constants.FMADRH) * 256;
						
						for (int i = 0 ; i < flashCounter ; i++)
							code(base+i,flashBuffer[i]);
						break;
						
				}
		}
			
	}
	
	public String toString()
	{
		return "Philips 89LPC900 $Id: LPC900.java 74 2010-07-07 05:50:03Z mviara $";
	}
}

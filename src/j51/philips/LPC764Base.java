/**
 * $Id: LPC764Base.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.philips;

import j51.intel.*;


/**
 *
 * LPC764 Emulator emulated peripheral :
 * 
 * 4KB OTP	Persistent
 * EEPROM	Persistent
 * JUART
 * TIMER
 * 
 * @author Mario Viara
 * @version 1.00
 */
public class LPC764Base extends MCS51 implements LPC764Constants , SfrWriteListener, MemoryReadListener, MemoryWriteListener
{
	private PersistentMemory eeprom;

	static public final int EEPROM_START = 0xfc00;
	static public final int EEPROM_SIZE  = 0x0400;
	static public final int EEPROM_UCFG1 = 0xfd00;
	static public final int EEPROM_UCFG2 = 0xfd01;
	
	public LPC764Base(String name)
	{
		super(6000000);
		machineCycle = 6;

		addSfrWriteListener(AUXR1,this);


		eeprom = new PersistentMemory(name,"eeprom",768);

		Code c = new FlashCode(name,4096);
		setCode(c);

		for (int i = 0 ; i < EEPROM_SIZE ; i++)
		{
			c.addMemoryWriteListener(EEPROM_START+i,this);
			c.addMemoryReadListener(EEPROM_START+i,this);
		}

		
		addPeripheral(new JUart());
		addPeripheral(new j51.intel.Timer());

		setSfrName(AUXR1,"AUXR1");

	}



	public void writeMemory(int addr,int newValue,int oldValue)
	{
		addr -= EEPROM_START;
		eeprom.write(addr,newValue);
	}

	public int readMemory(int addr,int value)
	{
		addr -= EEPROM_START;

		return eeprom.read(addr);

	}

	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	AUXR1:
				// Bit 2 is always 0
				v &= 0xfb;
				sfr(AUXR1,v);
				swapDptr(v & 1);

				// Reset if bit 3 is set
				if ((v & 0x08) != 0)
					reset();
				break;
		}
	}
	public String toString()
	{
		return "LPC764 $Id: LPC764Base.java 56 2010-06-24 20:06:35Z mviara $";
	}
	
}

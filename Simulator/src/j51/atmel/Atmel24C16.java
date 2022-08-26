/**
 * $Id: Atmel24C16.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.atmel;

import j51.intel.*;
import j51.i2c.*;
import j51.util.Logger;
import j51.util.Hex;

/**
 * Base class for ATMEL 24C16 EEPROM at I2C Address A0.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	Added support for new I2c implementation.
 */
public  class Atmel24C16 extends PersistentMemory implements I2cSlave
{
	private static Logger log = Logger.getLogger(Atmel24C16.class);
	private int address;

	public Atmel24C16()
	{
		super("AT24C16","eeprom",2048);
	}

	/**
	 * Check if the memory is addressed.
	 *
	 * @author Mario Viara
	 * @versio 1.00
	 *
	 * @since 1.01
	 */
	public boolean i2cAddress(int address)
	{
		return (address & 0xF0) == 0xA0;
	}

	public boolean i2cWrite(int count,int value) 
	{
		switch (count)
		{
			default:
				log.fine("Write "+Hex.bin2byte(value)+" at "+Hex.bin2word(address));
				write(address++,value);
				break;
			case	0:
				address = ((value >> 1) & 0x07) * 256 | (address & 0xff);
				//log.notice("Set address "+Hex.formatWord(address));
				break;
			case	1:
				address = (address & 0xff00 ) | value;
				//log.notice("Set address "+Hex.formatWord(address));
		}

		return true;
	}


	public int i2cRead(int count) 
	{

		int value = read(address);
		log.fine("Read "+Hex.bin2byte(value)+" at "+Hex.bin2word(address));
		address++;
		return value;
	}

	
}



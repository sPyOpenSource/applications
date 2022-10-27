/**
 * $Id: Diseqc.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.philips.diseqc;

import j51.intel.*;
import j51.philips.*;
import j51.atmel.Atmel24C16;
import j51.device.i2c.I2cBus;


/**
 * Diseqc Main class
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Diseqc extends LPC764Base
{
	
	Atmel24C16 eeprom;
	I2cBus i2c;
	OpenCollectorMemoryBit scl;
	OpenCollectorMemoryBit sda;
	
	public Diseqc() throws Exception
	{
		super("DISEQC");
		setOscillator(11059200);

		scl = createSfrBitOc(P0,6);
		sda = createSfrBitOc(P0,7);
		eeprom = new Atmel24C16();
		i2c = new I2cBus(scl,sda);
		i2c.addSlave(eeprom);
		addPeripheral(new DiseqcPeripheral());
		addPeripheral(i2c);
		addPeripheral(new LPC764Ports());
	}

	public int eeprom(int add)
	{
		add *= 2;
		int value = eeprom.read(add) << 8;
		value |= eeprom.read(add+1);
		

		return value;
	}
	
        @Override
	public String toString()
	{
		return "Diseqc 1.2 $Id: Diseqc.java 62 2010-06-29 22:06:12Z mviara $";
	}
				
}

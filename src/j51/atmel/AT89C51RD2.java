/**
 * $Id: AT89C51RD2.java 49 2010-06-23 08:38:02Z mviara $
 */
package j51.atmel;

import j51.intel.*;
import j51.util.Hex;


/**
 *
 * Atmel 89C51Rxx cpu.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	Added name for SFR.
 *
 */
public class AT89C51RD2 extends MCS51 implements AT89C51RD2Constants,
						SfrWriteListener,
						CallListener
{
	public AT89C51RD2() throws Exception
	{
		setXdataSize(2*1024);
		
		
		setCode(new FlashCode("P89C51RD2",64*1024));
		addPeripheral(new Timer());
		addPeripheral(new Timer2());
		addSfrWriteListener(AUXR1,this);

		setCallListener(0xfff0,this);

		setSfrName(P4,		"P4");
		setSfrName(P5,		"P5");
		setSfrName(AUXR1,	"AUXR1");
		
	}

	/**
	 * EEPROM API
	 */
	public void call(MCS51 _cpu,int pc) throws Exception
	{

		switch (r(1))
		{
			case	9:
				int dest   = getDptr(0);
				int source = getDptr(1);

				for (int i = 0 ; i < acc() ; i++)
					code(dest+i,xdata(source+i));
				acc(0);
				break;
			default:
				throw new Exception("API R1 "+r(1)+" A = "+acc()+" DPTR0 = "+Hex.bin2word(getDptr(0))+" DPTR1 = "+Hex.bin2word(getDptr(1)));
				
		}
	}
	
	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	AUXR1:

				// Bit 1 is always 0
				v &= 0xfd;
				sfr(AUXR1,v);
				swapDptr(v & 1);
				break;
		}
	}
	
}
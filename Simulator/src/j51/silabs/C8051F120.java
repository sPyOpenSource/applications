/**
 * $Id: C8051F120.java,v 1.2 2007/03/26 07:42:09 mviara Exp $
 */

package j51.silabs;

import j51.intel.*;

interface C8051F120Constants
{
	public final int EMIOCN	= 0xa2;
	public final int PSBANK = 0xb1;
	
}

/**
 * Silicon Laboratories C8051Fxxx.
 * 
 * Supported peripheral
 * 
 * - MOVX @Rx Hi byte of address in register EMIOCN
 * 
 * @author Mario Viara
 * @version 1.00
 * 
 */
public class C8051F120 extends MCS51 implements C8051F120Constants
{
    
	private final FlashCode		flash;
	private final PersistentMemory	scratch;
	
	C8051F120()
	{
		flash	= new FlashCode("C8051F120", 128 * 1024)
		{
                        @Override
			public int getCode(int addr,boolean fetch)
			{
				return super.getCode(translate(addr, fetch), fetch);
			}
			
		};
		
		scratch = new PersistentMemory("C8051F120","flash",256);
		
		setXdataSize(8192);
		setSfrXdataHi(EMIOCN);
		
		setSfrName(EMIOCN,	"EMIOCN");
		setSfrName(PSBANK,	"PSBANK");
	}

        @Override
	public void reset()
	{
		super.reset();
		sfr(PSBANK,0x11);
	}

	int translate(int addr, boolean fetch)
	{

		if (addr > 0x8000)
		{
			int psBank = sfr(PSBANK);
			int bank;
			addr &= 0x7fff;
			if (fetch)
				bank = psBank & 0x03;
			else
				bank = (psBank >> 4 ) & 0x03;
			addr += bank * 0x8000;
		}

		return addr;
	}
	
}

/**
 * $Id: Test.java,v 1.1 2007/02/16 07:50:23 mviara Exp $
 * $Name:  $
 *
 *
 * Bytechnology S400
 * 
 * $Log: Test.java,v $
 * Revision 1.1  2007/02/16 07:50:23  mviara
 * *** empty log message ***
 *
 * Revision 1.2  2005/11/29 03:47:03  mviara
 * Added same bit name.
 *
 * Revision 1.1  2005/11/27 23:59:39  mviara
 * Preliminary version of S400.
 *
 * 
 */
package j51.test;

import j51.intel.*;


class G128x64 extends j51.lcd.GLcd implements MCS51Peripheral,
					      SfrWriteListener,
					      SfrReadListener
{
	private final int ADDL = 0xfd;
	private final int ADDH = 0xfe;
	private final int DATA = 0xff;
	
	MCS51 cpu;
	private int address = 0;
	
	public G128x64()
	{
		super(128, 64, 2);
	}

        @Override
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		cpu.addSfrWriteListener(ADDL,this);
		cpu.addSfrWriteListener(ADDH,this);
		cpu.addSfrWriteListener(DATA,this);

		cpu.addSfrReadListener(DATA,this);
	}

        @Override
	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	ADDL:
				address &= 0xff00;
				address |= v & 0xff;
				break;
			case	ADDH:
				address &= 0x00ff;
				address |= v << 8;
				break;
			case	DATA:
				setMemory(address,(byte)(v & 0xff));
				break;
		}
	}

        @Override
	public int sfrRead(int r)
	{
		return getMemory(address) & 0xff;
	}
}

public class Test extends j51.intel.P8051
{
	public Test() throws Exception
	{
		setOscillator(11184000);
		addPeripheral(new G128x64());
	}
	
}



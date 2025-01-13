/**
 * $Id: Mon51.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.cmon51;

import j51.device.Timer;
import j51.intel.*;
import j51.swing.JPort;
import j51.swing.JUartTA;

/**
 * 
 * Special code memory to share XDATA and CODE space. The CODE
 * is mapped from 0000-7FFF in ROM and from 8000 to FFFF in
 * XDATA the XDATA is always mapped from 0000-7FFF.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.03
 */

public class Mon51 extends MCS51
{
	class CodeMon51 extends FlashCode
	{
		CodeMon51()
		{
			super("CMON51", 0x8000);
		}
		
                @Override
		public void setCode(int addr, int value)
		{
		
			if (addr >= 0x8000)
				xdata(addr, value);
                        else
				super.setCode(addr, value);
		}

                @Override
		public int getCode(int addr, boolean move)
		{
			if (addr >= 0x8000)
				return xdata(addr);
			else
				return super.getCode(addr, move);
		}

                @Override
		public int getCodeSize()
		{
			return 0x10000;
		}
	}

        @Override
	public int xdata(int add)
	{
		return super.xdata(add & 0x7fff);
	}

        @Override
	public void xdata(int add, int value)
	{
		super.xdata(add & 0x7fff, value);
	}

	public Mon51() throws Exception
	{
		addPeripheral(new Timer());
		addPeripheral(new JPort(4));
		addPeripheral(new JUartTA());
		setCode(new CodeMon51());
		setXdataSize(0x8000);
		setOscillator(22118400);
	}
}

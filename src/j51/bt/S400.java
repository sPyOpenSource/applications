/**
 * $Id: S400.java,v 1.2 2005/11/29 03:47:03 mviara Exp $
 */

package j51.bt;

import j51.intel.*;
import j51.atmel.*;


class LCD2x16 extends j51.lcd.JLcd implements MCS51Peripheral,j51.atmel.AT89C51RD2Constants,SfrWriteListener
{
	MCS51 cpu;
	int oldP5;

	public final int DISPLAY_RS =	0x01;
	public final int DISPLAY_RW =	0x02;
	public final int DISPLAY_EN =	0x04;
	public final int DISPLAY_RD =	0x80;		

	public LCD2x16()
	{
		super(2,16,2);
	}

	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;

		cpu.addSfrWriteListener(P5,this);
		cpu.addSfrWriteListener(P4,this);
		oldP5 = 0;
	}

	public void sfrWrite(int r,int v)
	{
		if (r == P4)
		{
			if (cpu.sfrIsSet(P5,DISPLAY_RW|DISPLAY_EN))
				cpu.sfrReset(P4,DISPLAY_RD);
			return;
		}

		if (cpu.sfrIsSet(P5,DISPLAY_EN))
		{
			// Read ?
			if (cpu.sfrIsSet(P5,DISPLAY_RW))
			{
				cpu.sfrReset(P4,DISPLAY_RD);
			}
			else // Write
			{

				if ((oldP5 & DISPLAY_EN) == 0)
				{
					int value = cpu.sfr(P4);
					if (cpu.sfrIsSet(P5,DISPLAY_RS))
						data(value);
					else
						cmd(value);
				}
			}
		}
		oldP5 = v;
	}
}

/**
 *
 * Bytechnology S400
 * 
 * @author Mario Viara
 * @version 1.00
 * 
 */
public class S400 extends j51.atmel.AT89C51RD2
{
	public S400() throws Exception
	{
		setOscillator(11184000);
		addPeripheral(new LCD2x16());
		addPeripheral(new JUart());
		AT89Port6 port = new AT89Port6();
		addPeripheral(port);
		port.setPortName(2,2,"Red led");
		port.setPortName(2,3,"Green led");
		port.setPortName(3,4,"Buzzer");
		port.setPortName(3,5,"RTS");

	}
	
}



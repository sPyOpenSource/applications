/**
 * $Id: Uart.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.device;

import j51.intel.*;
import java.awt.*;
import javax.swing.*;
import j51.swing.*;


/**
 *
 * Standard 8051 uart implementation, no baud rate are used but only
 * the send and receive char from the SBUF register.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Uart extends JPanel implements MCS51Peripheral,
	SfrWriteListener,SfrReadListener,MCS51Constants,
	InterruptSource,ResetListener
{
	private int sbuf = '?';
	private boolean sbufReaded;
	protected MCS51 cpu;
	private final byte inBuffer[] = new byte[1024*1024];
	int inPs,inPl;
	
	public Uart()
	{
		super(new BorderLayout());
		
		JFactory.setTitle(this,"Uart");

	}

	public void reset(MCS51 cpu)
	{
		inPs = inPl = 0;
		sbufReaded = true;
	}
	
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		cpu.addSfrWriteListener(MCS51Constants.SBUF,this);
		cpu.addSfrReadListener(MCS51Constants.SBUF,this);
		cpu.addInterruptSource(MCS51Constants.IE,this,"UART0");
		cpu.addInterruptSource(MCS51Constants.SCON,this,"UART0");
		cpu.addResetListener(this);
	}

	public int sfrRead(int r)
	{
		int value;
		
		switch (r)
		{
			case	SBUF:
					value = sbuf;
					sbufReaded = true;
					rxWakeup();
					return value;

		}
		
		return 0;
	}

	public void sfrWrite(int r,int v)
	{
		byte b[] = new byte[1];
		switch (r)
		{
			case	SBUF:
					sendChar(v);
					cpu.sfrSet(SCON,SCON_TI);
					
					break;
		}
	}

	
	public int getInterruptVector()
	{
		return 0x0023;
	}
	
	public void interruptStart()
	{
	}

	public void interruptStop()
	{
	}

	public boolean interruptCondition()
	{
		return (cpu.sfr(IE) & IE_ES) != 0 && (((cpu.sfr(SCON) & SCON_RI) != 0) ||
                        ((cpu.sfr(SCON) & SCON_TI) != 0));
	}

	protected void sendChar(int value)
	{
	}

	private synchronized void rxWakeup()
	{
		if (inPl == inPs)
			return;
		
		if (cpu.sfrIsSet(SCON,SCON_RI))
			return;
		
		if (!sbufReaded)
			return;

		sbuf = inBuffer[inPl] & 0xff;
		sbufReaded = false;
		cpu.sfrSet(SCON,SCON_RI);
		if (++inPl >= inBuffer.length)
			inPl = 0;
	}
	
	protected synchronized void recvChar(int value)
	{
		int newPs = inPs + 1;
		
		if (newPs > inBuffer.length)
			newPs = 0;
		if (newPs == inPl)
			return;
		inBuffer[inPs] = (byte)value;
		inPs = newPs;
		rxWakeup();
	}
	
}


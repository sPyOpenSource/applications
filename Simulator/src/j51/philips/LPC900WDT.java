/**
 * $Id: LPC900WDT.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.philips;

import j51.intel.*;

/**
 * Watch Dog for LPX9xx series.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class LPC900WDT extends AbstractInterruptSource implements MCS51Peripheral,LPC900Constants,
ResetListener,SfrWriteListener,AsyncTimerListener
{

	private int wdl;
	private LPC900 cpu;
	private boolean running = false;
	private boolean feed = false;
	private int wdClock;

	public LPC900WDT()
	{
		super(0x53);
	}

	public void registerCpu(MCS51 _cpu)
	{
		this.cpu = (LPC900)_cpu;

		cpu.setSfrName(WDCON,  "WDCON");
		cpu.setSfrName(WDFEED1,"WDFEED1");
		cpu.setSfrName(WDFEED2,"WDFEED2");
		cpu.setSfrName(WDL,    "WDL");

		cpu.addSfrWriteListener(WDCON,this);
		cpu.addSfrWriteListener(WDFEED1,this);
		cpu.addSfrWriteListener(WDFEED2,this);

		cpu.addInterruptSource(MCS51Constants.IE,this);
		cpu.addInterruptSource(WDCON,this);

		cpu.addResetListener(this);
	}

	public void reset(MCS51 _cpu)
	{
		running = feed = false;
		cpu.sfr(WDL,0xFF);
		cpu.sfr(WDCON,0xE7);
		wdl = 0xff;

		// Call the write listener because under reset is not
		// called but the WD must be enabled any way !!!
		sfrWrite(WDCON,0xE7);
	}

	public void sfrWrite(int r,int v)
	{
		switch (r)
		{
			case	LPC900Constants.WDFEED1:
				if (v == 0xA5)
					feed = true;
				else
					feed = false;
				break;

			case	LPC900Constants.WDFEED2:
				if (v == 0x5a && feed)
				{
					cpu.sfrReset(WDCON,WDCON_WDTOF);
					wdl = cpu.sfr(WDL);
				}
				feed = false;
				break;

			case	LPC900Constants.WDCON:
				// If WDTE and WDSE are enable
				// WDCLK = 1
				// WDL can be written a once
				// WDRUN is forced to 1
				if ((cpu.miscRead(UCFG1) & (UCFG1_WDTE|UCFG1_WDSE)) == (UCFG1_WDTE|UCFG1_WDSE))
				{
					v |= WDCON_WDRUN|WDCON_WDCLK;
					cpu.sfr(WDCON,v);
				}

				v &= 0xff;
				wdClock = v >> 5;
				wdClock = 32 << wdClock;
				addTimer();
				break;
		}
	}


	public boolean interruptCondition()
	{
		if ((cpu.sfr(MCS51Constants.IE) & IE_WD) != 0 && (cpu.sfr(WDCON) & WDCON_WDTOF) != 0)
			return true;

		return false;
	}


	private void addTimer()
	{
		int timer = 0;
		if (running)
			return;

		if ((cpu.sfr(WDCON) & WDCON_WDRUN) == 0)
			return;

		running = true;

		// Watch dog internal timer (400 Khz) ?
		if ((cpu.sfr(WDCON) & WDCON_WDCLK) != 0)
		{
			timer =  cpu.getOscillator() / 400000;
			timer /= cpu.machineCycle();
		}
		else
			timer = 1;


		timer *= wdClock;


		cpu.addAsyncTimerListener(timer,this);
	}

	public void expired(MCS51 _cpu) throws Exception
	{
		running = false;

		wdl = (wdl - 1) & 0xff;

		if (wdl == 0)
		{
			cpu.sfrSet(WDCON,WDCON_WDTOF);

			wdl = cpu.sfr(WDL);

			if ((cpu.miscRead(UCFG1) & (UCFG1_WDTE)) == (UCFG1_WDTE))
			{
				throw new Exception("Watch Dog reset");
			}

		}

		addTimer();
	}
}


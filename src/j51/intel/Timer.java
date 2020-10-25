/**
 * $Id: Timer.java 75 2010-07-07 06:04:05Z mviara $
 */

package j51.intel;

import j51.util.Hex;

/**
 * 
 * Standard 8051 timer 0 and 1 implementation. Only mode 0,1,2 are
 * supported and only in the timer mode no counter or gate enable are
 * supported.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Timer implements MCS51Peripheral,MachineCyclesListener,MCS51Constants
{
	/**
	 * Implementation of single timer
	 */
	class SingleTimer extends AbstractInterruptSource implements MCS51Constants
	{
		private MCS51 cpu;
		private int tconShift;
		private int tmodShift;
		private int TH;
		private int TL;
		private int TF;
		private int ET;
		private int timer;
		private int tcon;
		private int tmod;
		
		SingleTimer(int timer)
		{
			super(0x0b + timer * 0x10);
			tconShift = 2 * timer;
			tmodShift = 4 * timer;
			TH = MCS51Constants.TH0 + timer;
			TL = MCS51Constants.TL0 + timer;
			TF = TCON_TF0 << tconShift;
			ET = timer == 0 ? IE_ET0 : IE_ET1;
			this.timer = timer;
			
		}


		void registerCpu(MCS51 cpu)
		{
			this.cpu = cpu;
			cpu.addInterruptSource(MCS51Constants.IE,this,"TIMER"+timer);
			cpu.addInterruptSource(MCS51Constants.TCON,this,"TIMER"+timer);

			cpu.addSfrWriteListener(MCS51Constants.TCON,new SfrWriteListener()
			{
				public void sfrWrite(int r,int v)
				{
					tcon = v >> tconShift;
					
				}
			});

			cpu.addSfrWriteListener(MCS51Constants.TMOD,new SfrWriteListener()
			{
				public void sfrWrite(int r,int v)
				{
					tmod = v >> tmodShift;

				}
			});

			cpu.addSfrReadListener(MCS51Constants.TH0+timer,new SfrReadListener()
			{
				public int sfrRead(int r)
				{
					return TH;
				}
			});

			cpu.addSfrWriteListener(MCS51Constants.TH0+timer,new SfrWriteListener()
			{
				public void sfrWrite(int r,int v)
				{
					TH = v;
				}
			});

			cpu.addSfrReadListener(MCS51Constants.TL0+timer,new SfrReadListener()
			{
				public int sfrRead(int r)
				{
					return TL;
				}
			});

			cpu.addSfrWriteListener(MCS51Constants.TL0+timer,new SfrWriteListener()
			{
				public void sfrWrite(int r,int v)
				{
					TL = v;
				}
			});
			
		}

		final void cycle(int n)
		{
			int tl,th;

			// Do nothing if timer not running
			if ((tcon & TCON_TR0) == 0)
				return;

	
			switch (tmod & (TMOD_T0_M0 | TMOD_T0_M1))
			{
				case	0:	// 13 bit timer
					tl = TL + n;
					TL = tl & 0x1f;

					if (tl > 0x1f)
					{
						TH = (TH + 1) & 0xff;
						if (TH == 0)
						{
							cpu.sfrSet(TCON,TF);
							//System.out.println("Timer"+timer+" TF13");
						}
					}

					break;

					// Mode 1
				case	TMOD_T0_M0:
					tl = TL + n;
					TL= tl & 0xff;
					if (tl > 255)
					{
						
						TH = (TH + 1) & 0xff;
						if (TH == 0)
						{
							cpu.sfrSet(TCON,TF);
						}
					}
					break;
					
					// Mode 2
				case	TMOD_T0_M1:
					tl = TL;
					
					while (n-- > 0)
					{
						tl = (tl + 1) & 0xff;

						if (tl == 0)
						{
							tl = TL = TH;
							cpu.sfrSet(TCON,TF << tconShift);
						}

					}
					TL = tl;
					break;

			}

		}

		public void interruptStart()
		{
			cpu.sfrReset(TCON,TF);
		}


		public boolean interruptCondition()
		{
			if ((cpu.sfr(IE) & ET) != 0 && (cpu.sfr(TCON) & TF) != 0)
				return true;

			return false;
		}


		public String toString()
		{
			return "Timer"+timer+" at "+Hex.bin2word(vector);
		}
	}
	
	private MCS51 cpu;
	private SingleTimer timer0 = new SingleTimer(0);
	private SingleTimer timer1 = new SingleTimer(1);
	
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		cpu.addMachineCycleListener(this);
		timer0.registerCpu(cpu);
		timer1.registerCpu(cpu);
	}
	
	public final void cycles(int n)
	{
		timer0.cycle(n);
		timer1.cycle(n);
	}
}

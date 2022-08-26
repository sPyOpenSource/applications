/**
 * $Id: Timer2.java 45 2010-06-22 20:53:26Z mviara $
 *  
 */

package j51.intel;

/**
 *
 * Standard Intel 8052 Timer2
 *
 * @author Mario Viara
 * @version 1.00
 */
public class Timer2 extends AbstractInterruptSource implements MCS51Peripheral , MachineCyclesListener
{
	MCS51 cpu;

	static public final int T2CON		= 0xc8;
	static public final int T2CON_TF2	= 0x80;
	static public final int T2CON_EXF2	= 0x80;
	static public final int T2CON_TR2	= 0x04;
	static public final int T2CON_CP	= 0x01;
	static public final int T2MOD		= 0xc9;
	static public final int T2MOD_DCEN	= 0x01;
	static public final int TL2		= 0xcc;
	static public final int TH2		= 0xcd;
	static public final int RCAP2H		= 0xcb;
	static public final int RCAP2L		= 0xca;
	static public final int IEN0		= 0xa8;
	static public final int IEN0_ET2	= 0x20;
	static public final int IEN1		= 0xb1;

	public Timer2()
	{
		super(0x2b);
	}

	public void sfrWrite(int r,int v)
	{

	}

	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;

		cpu.setSfrName(TL2,"TL2");
		cpu.setSfrName(TL2,"TH2");
		cpu.setSfrName(T2MOD,"T2MOD");
		cpu.setSfrName(T2CON,"T2CON");
		cpu.setSfrName(RCAP2H,"RCAP2H");
		cpu.setSfrName(RCAP2L,"RCAP2L");

		cpu.addInterruptSource(IEN0,this,"TIMER2");
		cpu.addInterruptSource(T2CON,this,"TIMER2");

		cpu.addMachineCycleListener(this);

	}

	public void cycles(int n)
	{

		// Do nothing if timer2 not running
		if (!cpu.sfrIsSet(T2CON,T2CON_TR2))
			return;

		int t2 = cpu.sfr16(TH2,TL2);

		t2 += n;

		if (t2 >= 0xffff)
		{
			cpu.sfrSet(T2CON,T2CON_TF2);
			if (!cpu.sfrIsSet(T2CON,T2CON_CP))
			{
				t2 = cpu.sfr(RCAP2H) * 256 + cpu.sfr(RCAP2L);
			}
		}

		cpu.sfr16(t2,TH2,TL2);
	}

	public void interruptStart()
	{
		cpu.sfrReset(T2CON,T2CON_TF2);
	}

	public boolean interruptCondition()
	{
		if (cpu.sfrIsSet(IEN0,IEN0_ET2) && cpu.sfrIsSet(T2CON,T2CON_TF2))
			return true;

		return false;
	}

}



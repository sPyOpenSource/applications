/**
 * $Id: LPC900Ports.java 48 2010-06-23 08:28:23Z mviara $
 */
package j51.philips;

import j51.intel.*;

/**
 * LPC900 standard ports.
 *
 * @author Mario Viara
 * @versin 1.00
 */
public class LPC900Ports extends JPort implements ResetListener
{
	LPC900Ports() throws Exception
	{
		super(4);
		setDisableMask(3,0xFC);

	}

	public void registerCpu(MCS51 cpu)
	{
		super.registerCpu(cpu);
		cpu.addResetListener(this);
	}

	public void reset(MCS51 cpu)
	{
		// In the LPC900 default port is Input
		cpu.sfr(MCS51Constants.P0M1,0xff);
		cpu.sfr(MCS51Constants.P0M2,0x00);
		cpu.sfr(MCS51Constants.P1M1,0xff);
		cpu.sfr(MCS51Constants.P1M2,0x00);
		cpu.sfr(MCS51Constants.P2M1,0xff);
		cpu.sfr(MCS51Constants.P2M2,0x00);
	}

}


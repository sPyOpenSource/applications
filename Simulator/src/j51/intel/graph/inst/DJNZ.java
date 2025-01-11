
package j51.intel.graph.inst;

import j51.intel.MCS51;
import j51.intel.graph.JR;

/**
 *
 * @author xuyi
 */
public abstract class DJNZ extends JR
{
	public DJNZ(int opcode,int len,int cycle,String desc)
	{
		super(opcode, len, cycle, desc);
	}

	protected final void jnz(MCS51 cpu,int pc,int value)
	{
		value &= 0xff;
		if (value != 0)
			jr(cpu, pc, cpu.code(pc + getLength() - 1));
	}
}


package jCPU.MCS51.graph.inst;

import jCPU.MCS51.CPU;
import jCPU.MCS51.graph.JR;

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

	protected final void jnz(CPU cpu,int pc,int value)
	{
		value &= 0xff;
		if (value != 0)
			jr(cpu, pc, cpu.code(pc + getLength() - 1));
	}
}

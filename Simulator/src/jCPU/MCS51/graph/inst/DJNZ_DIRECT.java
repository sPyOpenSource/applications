
package jCPU.MCS51.graph.inst;

import jCPU.MCS51.CPU;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DJNZ_DIRECT extends DJNZ
{
	public DJNZ_DIRECT()
	{
		super(0xd5,3,2,"DJNZ\tDIRECT,#OFFSET");
	}

	public void exec(iCPU cpu,int pc)
	{
		int address = cpu.code(pc+1);
		int value = cpu.getDirect(address) - 1;
		cpu.setDirect(address,value);
		jnz((CPU) cpu, pc, value);
	}
}

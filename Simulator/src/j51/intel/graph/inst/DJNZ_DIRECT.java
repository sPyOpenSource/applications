
package j51.intel.graph.inst;

import j51.intel.MCS51;
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
		jnz((MCS51) cpu, pc, value);
	}
}

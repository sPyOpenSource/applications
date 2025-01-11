
package j51.intel.graph.inst;

import j51.intel.graph.CJNE;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CJNE_A_DIRECT extends CJNE
{
	public CJNE_A_DIRECT()
	{
		super(0xb5,3,2,"CJNE\tA,DIRECT,#OFFSET");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cjne(cpu, pc, cpu.acc(), cpu.getDirectCODE(pc + 1), cpu.code(pc + 2));
	}
}

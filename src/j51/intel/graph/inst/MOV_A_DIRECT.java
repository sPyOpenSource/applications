
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_A_DIRECT extends AbstractOpcode
{
	public MOV_A_DIRECT()
	{
		super(0xe5,2,1,"MOV\tA,DIRECT");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.getDirectCODE(pc+1));
	}
}

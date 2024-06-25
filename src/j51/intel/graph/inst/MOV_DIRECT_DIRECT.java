
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DIRECT_DIRECT extends AbstractOpcode
{
	public MOV_DIRECT_DIRECT()
	{
		super(0x85,3,2,"MOV\tDIRECP,DIRECM");
	}

	public final void exec(iCPU cpu,int pc)
	{
		int source = cpu.code(pc+1);
		int dest = cpu.code(pc+2);
		cpu.setDirect(dest,cpu.getDirect(source));
	}
}

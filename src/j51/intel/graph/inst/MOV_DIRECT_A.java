
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DIRECT_A extends AbstractOpcode
{
	public MOV_DIRECT_A()
	{
		super(0xf5,2,1,"MOV\tDIRECT,A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.setDirect(cpu.code(pc+1),cpu.acc());
	}
}

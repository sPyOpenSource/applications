
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_C_BIT extends AbstractOpcode
{
	public MOV_C_BIT()
	{
		super(0xa2,2,1,"MOV\tC,#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(cpu.getBitCODE(pc+1));
	}
}

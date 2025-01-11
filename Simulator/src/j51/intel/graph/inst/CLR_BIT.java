
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CLR_BIT extends AbstractOpcode
{
	public CLR_BIT()
	{
		super(0xc2,2,1,"CLR\t#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc + 1), false);
	}
}

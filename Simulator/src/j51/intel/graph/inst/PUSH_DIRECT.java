
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class PUSH_DIRECT extends AbstractOpcode
{
	public PUSH_DIRECT()
	{
		super(0xc0, 2, 2, "PUSH\tDIRECT");
	}

        @Override
	public void exec(iCPU cpu, int pc) throws Exception
	{
		cpu.push(cpu.getDirectCODE(pc + 1));
	}
}

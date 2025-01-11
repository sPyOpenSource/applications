
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CLR_A extends AbstractOpcode
{
	public CLR_A()
	{
		super(0xe4, 1, 1, "CLR\tA");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cpu.acc((int)0);
	}
}

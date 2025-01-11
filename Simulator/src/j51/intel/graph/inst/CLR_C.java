
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CLR_C extends AbstractOpcode
{
	public CLR_C()
	{
		super(0xc3, 1, 1, "CLR\tC");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cpu.cy(false);
	}
}

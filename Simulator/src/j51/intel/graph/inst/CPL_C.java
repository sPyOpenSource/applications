
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CPL_C extends AbstractOpcode
{
	public CPL_C()
	{
		super(0xb3,1,1,"CPL\tC");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(!cpu.cy());
	}
}

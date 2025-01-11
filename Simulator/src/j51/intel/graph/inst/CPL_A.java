
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CPL_A extends AbstractOpcode
{
	public CPL_A()
	{
		super(0xf4,1,1,"CPL\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)~cpu.acc());
	}
}

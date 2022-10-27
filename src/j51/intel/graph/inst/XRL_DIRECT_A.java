
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XRL_DIRECT_A extends AbstractOpcode
{
	public XRL_DIRECT_A()
	{
		super(0x62,2,1,"XRL");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) ^ cpu.acc()));
	}
}

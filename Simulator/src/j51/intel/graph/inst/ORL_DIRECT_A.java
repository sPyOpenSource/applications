
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ORL_DIRECT_A extends AbstractOpcode
{
	public ORL_DIRECT_A()
	{
		super(0x42,2,1,"ORL\tDIRECT,A");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) | cpu.acc()));
	}
}

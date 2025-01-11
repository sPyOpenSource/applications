
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class POP_DIRECT extends AbstractOpcode
{
	public POP_DIRECT()
	{
		super(0xd0,2,2,"POP\tDIRECT");
	}

        @Override
	public void exec(iCPU cpu,int pc) throws Exception
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.pop());
	}
}

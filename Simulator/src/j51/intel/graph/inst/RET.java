
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RET extends AbstractOpcode
{
	public RET()
	{
		super(0x22,1,2,"RET");
	}

        @Override
	public void exec(iCPU cpu,int pc) throws Exception
	{
		cpu.pc(cpu.popw());
	}
}

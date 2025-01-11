
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class SETB_BIT extends AbstractOpcode
{
	public SETB_BIT()
	{
		super(0xd2,2,1,"SETB\t#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc+1),true);
	}
}

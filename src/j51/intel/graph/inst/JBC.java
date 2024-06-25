
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JBC extends JR
{
	public JBC()
	{
		super(0x10,3,2,"JBC\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		if (cpu.getBit(add))
		{
			cpu.setBit(add,false);
			jr(cpu,pc,cpu.code(pc+2));
		}
	}
}

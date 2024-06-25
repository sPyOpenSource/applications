
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JB extends JR
{
	public JB()
	{
		super(0x20,3,2,"JB\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu, int pc)
	{
		if (cpu.getBitCODE(pc + 1))
			jr(cpu, pc, cpu.code(pc + 2));
	}
}

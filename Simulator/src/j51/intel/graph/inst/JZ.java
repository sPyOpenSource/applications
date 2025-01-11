
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JZ extends JR
{
	public JZ()
	{
		super(0x60,2,2,"JZ\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.acc() == 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}

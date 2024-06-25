
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JNB extends JR
{
	public JNB()
	{
		super(0x30,3,2,"JNB\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		boolean bit = cpu.getBitCODE(pc+1);

		if (!bit)
			jr(cpu,pc,cpu.code(pc+2));
	}
}

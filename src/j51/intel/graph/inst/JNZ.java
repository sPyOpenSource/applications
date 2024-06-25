
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JNZ extends JR
{
	public JNZ()
	{
		super(0x70,2,2,"JNZ\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.acc() != 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}

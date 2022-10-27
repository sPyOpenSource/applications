
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class SJMP extends JR
{
	public SJMP()
	{
		super(0x80,2,2,"SJMP\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		jr(cpu,pc,cpu.code(pc+1));
	}
}

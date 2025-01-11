
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JC extends JR
{
	public JC()
	{
		super(0x40,2,2,"JC\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}

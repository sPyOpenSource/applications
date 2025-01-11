
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JNC extends JR
{
	public JNC()
	{
		super(0x50,2,2,"JNC\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (!cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class SETB_C extends AbstractOpcode
{
	public SETB_C()
	{
		super(0xd3,1,1,"SETB\tC");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(true);
	}
}

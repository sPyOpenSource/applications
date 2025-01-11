
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DEC_DIRECT extends AbstractOpcode
{
	public DEC_DIRECT()
	{
		super(0x15,2,1,"DEC\tDIRECT");
	}

	public void exec(iCPU cpu,int pc)
	{
		int direct = cpu.code(pc+1);
		cpu.setDirect(direct,(int)(cpu.getDirect(direct) - 1));
	}

}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XRL_DIRECT_DATA extends AbstractOpcode
{
	public XRL_DIRECT_DATA()
	{
		super(0x63,3,2,"XRL\tDIRECT,#DATA8");
	}
	
        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) ^ cpu.code(pc+2)));
	}
}

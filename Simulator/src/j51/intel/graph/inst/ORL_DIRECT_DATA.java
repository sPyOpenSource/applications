
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ORL_DIRECT_DATA extends AbstractOpcode
{
	public ORL_DIRECT_DATA()
	{
		super(0x43,3,2,"ORL\tDIRECT,#DATA8");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,(int)(cpu.getDirect(add) | cpu.code(pc+2)));
	}
}

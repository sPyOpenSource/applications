
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ANL_DIRECT_DATA extends AbstractOpcode
        
{
	public ANL_DIRECT_DATA()
	{
		super(0x53, 3, 2, "ANL");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		int add = cpu.code(pc + 1);
		cpu.setDirect(add, (int)(cpu.getDirect(add) & cpu.code(pc + 2)));
	}

        @Override
	public String toString()
	{
		return description + "\tDIRECT,#DATA8";
	}

}

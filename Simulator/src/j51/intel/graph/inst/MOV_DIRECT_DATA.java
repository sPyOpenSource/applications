
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DIRECT_DATA extends AbstractOpcode
{
	public MOV_DIRECT_DATA()
	{
		super(0x75,3,2,"MOV\tDIRECT,#DATA8");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.code(pc+2));
	}
}

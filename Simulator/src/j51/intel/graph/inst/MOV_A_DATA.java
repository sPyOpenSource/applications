
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_A_DATA extends AbstractOpcode
{
	public MOV_A_DATA()
	{
		super(0x74,2,1,"MOV\tA,#DATA8");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1));
	}
}

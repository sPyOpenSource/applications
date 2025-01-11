
package j51.intel.graph.inst;

import j51.intel.graph.CJNE;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CJNE_A_DATA extends CJNE
{
	public CJNE_A_DATA()
	{
		super(0xb4,3,2,"CJNE\tA,#DATA8,#OFFSET");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.acc(),cpu.code(pc+1),cpu.code(pc+2));
	}
}


package j51.intel.graph.inst;

import j51.intel.graph.CJNE;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CJNE_RI_DATA extends CJNE
{
	public CJNE_RI_DATA(int r)
	{
		super(0xb6+r,3,2,"CJNE");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu, pc, cpu.idata(cpu.r((int)(opcode & 1))), cpu.code(pc + 1), cpu.code(pc + 2));
	}

        @Override
	public String toString()
	{
		return description + "\t@R" + (opcode & 1) + ",#DATA8,#OFFSET";
	}
}

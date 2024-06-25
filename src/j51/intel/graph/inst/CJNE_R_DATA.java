
package j51.intel.graph.inst;

import j51.intel.graph.CJNE;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CJNE_R_DATA extends CJNE
{
	public CJNE_R_DATA(int r)
	{
		super(0xb8+r,3,2,"CJNE");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.r((int)(opcode & 7)),cpu.code(pc+1),cpu.code(pc+2));
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8,#OFFSET";
	}
}

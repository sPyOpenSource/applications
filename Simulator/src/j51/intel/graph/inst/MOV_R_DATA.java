
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_R_DATA extends AbstractOpcode
{
	public MOV_R_DATA(int r)
	{
		super(0x78|r,2,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.code(pc+1));
	}
	
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8";
	}

}

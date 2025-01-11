
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_A_RI extends AbstractOpcode
{
	public MOV_A_RI(int r)
	{
		super(0xe6|r,1,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.idata(cpu.r((int)(opcode & 1))));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

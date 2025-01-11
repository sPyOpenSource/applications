
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_A_R extends AbstractOpcode
{
	public MOV_A_R(int r)
	{
		super(0xe8|r,1,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.r((int)(opcode & 7)));
	}

	public String toString()
	{
		return description+"\tA,R"+(opcode & 7);
	}

}

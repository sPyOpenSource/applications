
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_RI_A extends AbstractOpcode
{
	public MOV_RI_A(int r)
	{
		super(0xf6|r,1,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_RI_DIRECT extends AbstractOpcode
{
	public MOV_RI_DIRECT(int r)
	{
		super(0xa6|r,2,2,"MOV");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,cpu.getDirectCODE(pc+1));
	}

        @Override
	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",DIRECT";
	}

}

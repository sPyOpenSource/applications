
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DIRECT_RI extends AbstractOpcode
{
	public MOV_DIRECT_RI(int r)
	{
		super(0x86|r,2,2,"MOV");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.idata(cpu.r((int)(opcode & 1))));
	}

        @Override
	public String toString()
	{
		return description+"\tDIRECT,@R"+(opcode & 1);
	}
}

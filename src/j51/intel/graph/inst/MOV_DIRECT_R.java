
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DIRECT_R extends AbstractOpcode
{
	public MOV_DIRECT_R(int r)
	{
		super(0x88|r,2,2,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.r((int)(opcode & 7)));
	}

	public String toString()
	{
		return description+"\tDIRECT,R"+(opcode & 7);
	}

}

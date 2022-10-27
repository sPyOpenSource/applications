
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_R_A extends AbstractOpcode
{
	public MOV_R_A(int r)
	{
		super(0xf8|r,1,1,"MOV");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.acc());
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",A";
	}

}

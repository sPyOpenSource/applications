
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_R_DIRECT extends AbstractOpcode
{
    
	public MOV_R_DIRECT(int r)
	{
		super(0xa8|r,2,2,"MOV");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.getDirectCODE(pc+1));
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",DIRECT";
	}

}

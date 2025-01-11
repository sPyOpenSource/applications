
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_RI_DATA extends AbstractOpcode
{
    
	public MOV_RI_DATA(int r)
	{
		super(0x76|r,2,1,"MOV");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,(int)(cpu.code(pc+1)));
	}

        @Override
	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",#DATA8";
	}

}

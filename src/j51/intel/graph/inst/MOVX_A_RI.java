
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVX_A_RI extends AbstractOpcode
{
	public MOVX_A_RI(int r)
	{
		super(0xe2|r,1,2,"MOVX");
	}

	public final void exec(iCPU cpu,int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.acc(cpu.xdata(offset));
	}
        
	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

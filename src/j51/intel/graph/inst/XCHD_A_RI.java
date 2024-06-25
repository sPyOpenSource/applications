
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XCHD_A_RI extends AbstractOpcode
{
	public XCHD_A_RI(int r)
	{
		super(0xd6|r,1,1,"XCHD");
	}

	public void exec(iCPU cpu,int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc((int)((cpu.acc() & 0xf0) | (cpu.idata(r) & 0x0f)));
		cpu.idata(r,(int)((cpu.idata(r) & 0xf0) | (tmp) & 0x0f));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVX_RI_A extends AbstractOpcode
{
	public MOVX_RI_A(int r)
	{
		super(0xf2|r,1,2,"MOVX");
	}

	public final void exec(iCPU cpu, int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.xdata(offset,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}

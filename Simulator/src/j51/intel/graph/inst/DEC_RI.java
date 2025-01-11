
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DEC_RI extends AbstractOpcode
{
	public DEC_RI(int r)
	{
		super(0x16|r,1,1,"DEC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int address = cpu.r((int)(opcode & 1));
		cpu.idata(address,cpu.idata(address ) -1 );
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

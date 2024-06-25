
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XCH_A_RI extends AbstractOpcode
{
	public XCH_A_RI(int r)
	{
		super(0xc6|r,1,1,"XCH");
	}

	public void exec(iCPU cpu, int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc(cpu.idata(r));
		cpu.idata(r, tmp);
	}

	public String toString()
	{
		return description + "\tA,@R" + (opcode & 1);
	}

}

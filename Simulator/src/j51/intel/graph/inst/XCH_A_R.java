
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XCH_A_R extends AbstractOpcode
{
	public XCH_A_R(int r)
	{
		super(0xc8|r,1,1,"XCH");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		int tmp = cpu.acc();
		cpu.acc(cpu.r(r));
		cpu.r(r,tmp);
	}

	public String toString()
	{
		return description + "\tA,R" + (opcode & 7);
	}

}

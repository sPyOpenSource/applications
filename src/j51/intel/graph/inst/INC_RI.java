
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_RI extends AbstractOpcode
{
	public INC_RI(int r)
	{
		super(6|r,1,1,"INC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int i = cpu.r((int)(opcode & 1));
		cpu.setDirect(i,(int)(cpu.getDirect(i) + 1));
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

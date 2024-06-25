
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_R extends AbstractOpcode
{
	public INC_R(int r)
	{
		super(8|r,1,1,"INC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r)+1));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}

}
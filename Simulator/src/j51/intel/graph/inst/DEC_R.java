
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DEC_R extends AbstractOpcode
{
	public DEC_R(int r)
	{
		super(0x18|r,1,1,"DEC");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r) - 1));
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CPL_BIT extends AbstractOpcode
{
	public CPL_BIT()
	{
		super(0xb2,2,1,"CPL\t#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int bit = cpu.code(pc+1);
		cpu.setBit(bit, !cpu.getBit(bit));
	}
}

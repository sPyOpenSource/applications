
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CPL_A extends AbstractOpcode
{
	public CPL_A()
	{
		super(0xf4,1,1,"CPL\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)~cpu.acc());
	}
}


class CPL_C extends AbstractOpcode
{
	public CPL_C()
	{
		super(0xb3,1,1,"CPL\tC");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(!cpu.cy());
	}
}

class CPL_BIT extends AbstractOpcode
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

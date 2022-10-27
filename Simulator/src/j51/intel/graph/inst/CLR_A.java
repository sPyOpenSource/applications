
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CLR_A extends AbstractOpcode
{
	public CLR_A()
	{
		super(0xe4, 1, 1, "CLR\tA");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cpu.acc((int)0);
	}
}

class CLR_C extends AbstractOpcode
{
	public CLR_C()
	{
		super(0xc3, 1, 1, "CLR\tC");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cpu.cy(false);
	}
}

class CLR_BIT extends AbstractOpcode
{
	public CLR_BIT()
	{
		super(0xc2,2,1,"CLR\t#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc + 1), false);
	}
}

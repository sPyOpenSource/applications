
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class SWAP_A extends AbstractOpcode
{
	public SWAP_A()
	{
		super(0xc4,1,1,"SWAP\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();
		cpu.acc((int)((a >> 4) & 0x0f | (a << 4)));
	}

}

class PUSH_DIRECT extends AbstractOpcode
{
	public PUSH_DIRECT()
	{
		super(0xc0, 2, 2, "PUSH\tDIRECT");
	}

        @Override
	public void exec(iCPU cpu, int pc) throws Exception
	{
		cpu.push(cpu.getDirectCODE(pc + 1));
	}
}

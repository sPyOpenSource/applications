
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

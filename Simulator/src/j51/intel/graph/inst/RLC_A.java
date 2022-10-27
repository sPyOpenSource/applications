
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RLC_A extends AbstractOpcode
{
	public RLC_A()
	{
		super(0x33,1,1,"RLC\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a << 1;
		if (cpu.cy())
			a |= 1;
		cpu.cy((a & 0x100) != 0) ;
		cpu.acc(a);
	}
}

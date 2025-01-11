
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RR_A extends AbstractOpcode
{
	public RR_A()
	{
		super(0x3,1,1,"RR\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a >> 1;
		if ((cpu.acc() & 0x01) != 0)
			a |= 0x80;
		cpu.acc((int)a);
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RL_A extends AbstractOpcode
{
	public RL_A()
	{
		super(0x23,1,1,"RL\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a << 1;
		if ((cpu.acc() & 0x80) != 0){
			a |= 1;
                }
		cpu.acc((int)a);
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MUL_AB extends AbstractOpcode
{
	public MUL_AB()
	{
		super(0xa4,1,4,"MUL\tAB");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int value = cpu.acc() * cpu.b();

		cpu.b((int)(value >> 8));
		cpu.acc((int)value);
		cpu.cy();
		cpu.ov((value > 255));
	}
}

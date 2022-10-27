
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DA_A extends AbstractOpcode
{
	public DA_A()
	{
		super(0xd4,1,1,"DA\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();

		if ((a & 0x0f)	> 9 || cpu.ac())
		{
			a += 6;
			if ((a & 0xf0) != (cpu.acc() & 0xf0)){
				cpu.cy(true);
                        }
		}

		if ((a & 0xf0) > 0x90 || cpu.cy())
		{
			a += 0x60;
			if (a  > 255)
				cpu.cy(true);
		}

		cpu.acc((int)a);
	}
}

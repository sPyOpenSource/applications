
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DIV_AB extends AbstractOpcode
{
	public DIV_AB()
	{
		super(0x84, 1, 4, "DIV\tAB");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		int a,b;

		a = cpu.acc();
		b = cpu.b();

		cpu.acc((int)(a / b));
		cpu.b((int)(a % b));
		cpu.cy(false);
		cpu.ov(false);
	}
}

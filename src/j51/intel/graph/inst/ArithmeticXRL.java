
package j51.intel.graph.inst;

import j51.intel.graph.ArithmeticOperation;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ArithmeticXRL implements ArithmeticOperation
{
        @Override
	public final void calc(iCPU cpu, int value)
	{
		cpu.acc((int)(cpu.acc() ^ value));
	}
}



package jCPU.MCS51.graph.inst;

import jCPU.MCS51.graph.ArithmeticOperation;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ArithmeticANL implements ArithmeticOperation
{
        @Override
	public final void calc(iCPU cpu, int value)
	{
		cpu.acc((int)(cpu.acc() & value));
	}
}

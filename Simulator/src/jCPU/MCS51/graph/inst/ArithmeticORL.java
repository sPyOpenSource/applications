
package jCPU.MCS51.graph.inst;

import jCPU.MCS51.graph.ArithmeticOperation;
import jCPU.iCPU;

public class ArithmeticORL implements ArithmeticOperation
{
        @Override
	public final void calc(iCPU cpu, int value)
	{
		cpu.acc((int)(cpu.acc() | value));
	}
}

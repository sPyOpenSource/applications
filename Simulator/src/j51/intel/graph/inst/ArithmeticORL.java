
package j51.intel.graph.inst;

import j51.intel.graph.ArithmeticOperation;
import jCPU.iCPU;

public class ArithmeticORL implements ArithmeticOperation
{
        @Override
	public final void calc(iCPU cpu, int value)
	{
		cpu.acc((int)(cpu.acc() | value));
	}
}

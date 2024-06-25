
package j51.intel.graph.inst;

import j51.intel.graph.ArithmeticOperation;
import jCPU.iCPU;

public class ArithmeticADD implements ArithmeticOperation
{
    
	protected int result;
	
	protected boolean op(int acc, int value, int c, int mask)
	{
		result = (acc & mask) + (value & mask) + c;
		return (result & (mask + 1)) != 0;
	}
	
	protected final void add(iCPU cpu, int value, int c)
	{
		int acc = cpu.acc();
		cpu.ac(op(acc, value, c, 0x0f));
		boolean cy7 = op(acc, value, c, 0x7F);
		cpu.cy(op(acc, value, c, 0xff));
		cpu.ov(cpu.cy() != cy7);
		cpu.acc(result);
	}
	
        @Override
	public void calc(iCPU cpu, int value)
	{
		add(cpu, value, 0);
	}

}
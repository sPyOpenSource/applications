
package j51.intel.graph.inst;

import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ArithmeticADDC extends ArithmeticADD
{

        @Override
	public void calc(iCPU cpu, int value)
	{
		int c = cpu.cy() ? 1 : 0;
		add(cpu, value, c);
	}

}

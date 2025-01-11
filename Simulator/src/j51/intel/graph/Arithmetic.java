
package j51.intel.graph;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public abstract class Arithmetic extends AbstractOpcode
{
	ArithmeticOperation op;
	
	public Arithmetic(int opcode, int length, ArithmeticOperation op, String name)
	{
		super(opcode, length, 1, name);
		this.op = op;
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		op.calc(cpu,getValue(cpu, pc));
	}

	public abstract int getValue(iCPU cpu, int pc);
}

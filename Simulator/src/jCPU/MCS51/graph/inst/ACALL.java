
package jCPU.MCS51.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.CallListener;
import jCPU.MCS51.CPU;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ACALL extends AbstractOpcode
{
	
	public ACALL(int opcode)
	{
		super(opcode, 2, 2, "ACALL\t#DATA12");
	}
	
	protected ACALL(int opcode, String name)
	{
		super(opcode, 2, 2, name);
	}
		
	protected final int getAddress(CPU cpu, int pc)
	{
		int add = cpu.code(pc + 1) | ((opcode << 3) & 0x700);
		add |= (pc + 2 )	& 0xF800;
		return add;
	}
	
        @Override
	public void exec(iCPU cpu, int pc) throws Exception
	{
		int address = getAddress((CPU) cpu, pc);
		CallListener l = cpu.getCallListener(address);
		if (l != null)
		{
			l.call((CPU) cpu, address);
		} else {
			cpu.pushw(pc + 2);
			cpu.pc(getAddress((CPU) cpu, pc));
		}
	}

}

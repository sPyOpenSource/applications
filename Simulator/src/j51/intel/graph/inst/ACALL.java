
package j51.intel.graph.inst;

import j51.intel.CallListener;
import j51.intel.MCS51;
import jCPU.AbstractOpcode;
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
		
	protected final int getAddress(MCS51 cpu, int pc)
	{
		int add = cpu.code(pc + 1) | ((opcode << 3) & 0x700);
		add |= (pc + 2 )	& 0xF800;
		return add;
	}
	
        @Override
	public void exec(iCPU cpu, int pc) throws Exception
	{
		int address = getAddress((MCS51) cpu, pc);
		CallListener l = cpu.getCallListener(address);
		if (l != null)
		{
			l.call((MCS51) cpu, address);
		} else {
			cpu.pushw(pc + 2);
			cpu.pc(getAddress((MCS51) cpu, pc));
		}
	}

}

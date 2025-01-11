
package j51.intel.graph;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public abstract class JR extends AbstractOpcode
{
	
	public JR(int opcode,int len,int cycle,String desc)
	{
		super(opcode,len,cycle,desc);
	}

	protected final void jr(iCPU cpu,int pc,int offset)
	{
		pc = pc + length;

		if (offset < 128)
			pc += offset;
		else
			pc -= 0x100 - offset;
		
		cpu.pc(pc);
	}
        
}

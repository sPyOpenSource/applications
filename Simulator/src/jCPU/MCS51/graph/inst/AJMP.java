
package jCPU.MCS51.graph.inst;

import jCPU.MCS51.CPU;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class AJMP extends ACALL
{
    
	public AJMP(int opcode)
	{
		super(opcode,"AJMP\t#DATA12");
	}


        @Override
	public final void exec(iCPU cpu, int pc)
	{

		cpu.pc(getAddress((CPU) cpu, pc));
	}
		
}

package j51.intel.graph.inst;

import j51.intel.MCS51;
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

		cpu.pc(getAddress((MCS51) cpu, pc));
	}
		
}
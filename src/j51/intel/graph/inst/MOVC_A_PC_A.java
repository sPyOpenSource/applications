
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVC_A_PC_A extends AbstractOpcode
{
	public MOVC_A_PC_A()
	{
		super(0x83,1,2,"MOVC\tA,@PC+A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1+cpu.acc()));
	}
}

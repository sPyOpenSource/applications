
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVC_A_DPTR_A extends AbstractOpcode
{
	public MOVC_A_DPTR_A()
	{
		super(0x93,1,2,"MOVC\tA,@DPTR+A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(cpu.dptr()+cpu.acc()));
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVX_DPTR_A extends AbstractOpcode
{
	public MOVX_DPTR_A()
	{
		super(0xf0,1,2,"MOVX\t@DPTR,A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.xdata(cpu.dptr(),cpu.acc());

	}
}

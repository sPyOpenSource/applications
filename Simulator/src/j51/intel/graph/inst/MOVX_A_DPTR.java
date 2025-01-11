
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOVX_A_DPTR extends AbstractOpcode
{
	public MOVX_A_DPTR()
	{
		super(0xe0,1,2,"MOVX\tA,@DPTR");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.xdata(cpu.dptr()));
	}
}

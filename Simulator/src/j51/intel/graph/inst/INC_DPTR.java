
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_DPTR extends AbstractOpcode
{
	public INC_DPTR()
	{
		super(0xa3,1,2,"INC\tDPTR");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.dptr(cpu.dptr()+1);
	}
}

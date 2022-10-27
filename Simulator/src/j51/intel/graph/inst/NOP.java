
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class NOP extends AbstractOpcode
{
	public NOP()
	{
		super(0,1,1,"NOP");
	}

        @Override
	public final void exec(iCPU cpu, int pc)
	{
		
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_DIRECT extends AbstractOpcode
{
	public INC_DIRECT()
	{
		super(5,2,1,"INC\tDIRECT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.code(pc+1);
		cpu.setDirect(a,(int)(cpu.getDirect(a)+1));
	}

}

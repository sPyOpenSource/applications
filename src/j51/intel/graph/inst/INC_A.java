
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_A extends AbstractOpcode
{
	public INC_A()
	{
		super(4,1,1,"INC\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc()+1));
	}
	
}

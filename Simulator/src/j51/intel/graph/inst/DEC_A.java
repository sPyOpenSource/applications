
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DEC_A extends AbstractOpcode
{
	public DEC_A()
	{
		super(0x14,1,1,"DEC\tA");
	}
	
        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc() - 1));
	}
	
}

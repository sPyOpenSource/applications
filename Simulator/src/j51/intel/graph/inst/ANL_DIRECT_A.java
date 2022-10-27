
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ANL_DIRECT_A extends AbstractOpcode
{
	public ANL_DIRECT_A()
	{
		super(0x52,2,1,"ANL");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.getDirect(add) & cpu.acc());
	}

        @Override
	public String toString()
	{
		return description + "\tA,DIRECT";
	}
}

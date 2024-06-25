
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ANL_C_NOT_DIRECT extends AbstractOpcode
{
	public ANL_C_NOT_DIRECT()
	{
		super(0xb0,2,2,"ANL");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(!(cpu.getBit(cpu.code(pc+1)) & cpu.cy()));
	}

        @Override
	public String toString()
	{
		return description+"\tC,NOT #BIT";
	}
}

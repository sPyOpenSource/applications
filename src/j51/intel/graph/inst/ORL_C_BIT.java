
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class ORL_C_BIT extends AbstractOpcode
{
	public ORL_C_BIT()
	{
		super(0x72,2,2,"ORL\tC,#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(cpu.getBitCODE(pc+1)|cpu.cy());
	}
}

class ORL_C_NBIT extends AbstractOpcode
{
	public ORL_C_NBIT()
	{
		super(0xA0,2,2,"ORL\tC,NOT #BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(cpu.cy() | !cpu.getBitCODE(pc+1));
	}
}

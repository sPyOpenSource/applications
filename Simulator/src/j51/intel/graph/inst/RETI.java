
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RETI extends AbstractOpcode
{
	public RETI()
	{
		super(0x32,1,2,"RETI");
	}

        @Override
	public void exec(iCPU cpu,int pc) throws Exception
	{
		cpu.pc(cpu.popw());
		cpu.eoi();
	}
}

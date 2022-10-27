
package j51.intel.graph.inst;

import j51.intel.CallListener;
import j51.intel.MCS51;
import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class LCALL extends AbstractOpcode
{
	
	public LCALL()
	{
		super(0x12,3,2,"LCALL\t#CODE16");
	}

        @Override
	public void exec(iCPU cpu,int pc) throws Exception
	{
		int address = cpu.code16(pc+1);
		CallListener l = cpu.getCallListener(address);

		if (l != null)
		{
			l.call((MCS51) cpu,address);
		} else {
			cpu.pushw(pc+3);
			cpu.pc(address);
		}
	}
	
}

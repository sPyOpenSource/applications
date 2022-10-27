
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class LJMP extends AbstractOpcode
{
	public LJMP()
	{
		super(0x2, 3, 2, "LJMP\t#CODE16");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cpu.pc(cpu.code16(pc + 1));
		//cpu.pc((cpu.code(pc+1) << 8) | cpu.code(pc+2));
	}

}

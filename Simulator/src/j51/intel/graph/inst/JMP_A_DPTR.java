
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JMP_A_DPTR extends AbstractOpcode
{
	public JMP_A_DPTR()
	{
		super(0x73,1,2,"JMP\t@A+DPTR");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		cpu.pc(cpu.dptr()+cpu.acc());
	}
}

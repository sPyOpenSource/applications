
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_BIT_C extends AbstractOpcode
{
	public MOV_BIT_C()
	{
		super(0x92,2,2,"MOV\t#BIT,C");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.setBit(cpu.code(pc+1),cpu.cy());
	}
}


package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_DPTR_DATA16 extends AbstractOpcode
{
	public MOV_DPTR_DATA16()
	{
		super(0x90,3,2,"MOV\tDPTR,#DATA16");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.dptr((cpu.code(pc+1) << 8) | cpu.code(pc+2));
	}
}

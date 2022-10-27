
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RRC_A extends AbstractOpcode
{
	public RRC_A()
	{
		super(0x13,1,1,"RRC\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.acc();

		a = a >> 1;
		if (cpu.cy()){
			a |= 0x80;
                }
		
		cpu.cy((cpu.acc() & 1) != 0);
			
		cpu.acc((int)a);
	}
}

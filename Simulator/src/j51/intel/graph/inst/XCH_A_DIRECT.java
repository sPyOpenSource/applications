
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XCH_A_DIRECT extends AbstractOpcode
{
	public XCH_A_DIRECT()
	{
		super(0xc5,2,1,"XCH\tA,DIRECT");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		int tmp = cpu.acc();
		cpu.acc(cpu.getDirect(add));
		cpu.setDirect(add,tmp);
	}

}

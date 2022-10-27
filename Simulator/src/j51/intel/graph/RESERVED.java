
package j51.intel.graph;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class RESERVED extends AbstractOpcode
{
	public RESERVED()
	{
		super(0xa5,1,1,"RESERVED");
	}

        @Override
	public void exec(iCPU cpu, int pc) throws Exception
	{
		throw new Exception("Invalid opcode : A5");
	}

}

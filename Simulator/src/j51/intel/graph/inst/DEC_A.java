
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DEC_A extends AbstractOpcode
{
	public DEC_A()
	{
		super(0x14,1,1,"DEC\tA");
	}
	
        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc() - 1));
	}
	
}

class DEC_R extends AbstractOpcode
{
	public DEC_R(int r)
	{
		super(0x18|r,1,1,"DEC");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r) - 1));
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}
}

class DEC_RI extends AbstractOpcode
{
	public DEC_RI(int r)
	{
		super(0x16|r,1,1,"DEC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int address = cpu.r((int)(opcode & 1));
		cpu.idata(address,cpu.idata(address ) -1 );
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

class DEC_DIRECT extends AbstractOpcode
{
	public DEC_DIRECT()
	{
		super(0x15,2,1,"DEC\tDIRECT");
	}

	public void exec(iCPU cpu,int pc)
	{
		int direct = cpu.code(pc+1);
		cpu.setDirect(direct,(int)(cpu.getDirect(direct) - 1));
	}

}

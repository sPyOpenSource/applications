
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class INC_A extends AbstractOpcode
{
	public INC_A()
	{
		super(4,1,1,"INC\tA");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.acc((int)(cpu.acc()+1));
	}
	
}

class INC_R extends AbstractOpcode
{
	INC_R(int r)
	{
		super(8|r,1,1,"INC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		cpu.r(r,(int)(cpu.r(r)+1));
	}

	public String toString()
	{
		return description+"\tR"+(opcode & 7);
	}

}

class INC_RI extends AbstractOpcode
{
	INC_RI(int r)
	{
		super(6|r,1,1,"INC");
	}

	public void exec(iCPU cpu,int pc)
	{
		int i = cpu.r((int)(opcode & 1));
		cpu.setDirect(i,(int)(cpu.getDirect(i) + 1));
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1);
	}

}

class INC_DIRECT extends AbstractOpcode
{
	INC_DIRECT()
	{
		super(5,2,1,"INC\tDIRECT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int a = cpu.code(pc+1);
		cpu.setDirect(a,(int)(cpu.getDirect(a)+1));
	}

}

class INC_DPTR extends AbstractOpcode
{
	INC_DPTR()
	{
		super(0xa3,1,2,"INC\tDPTR");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.dptr(cpu.dptr()+1);
	}
}

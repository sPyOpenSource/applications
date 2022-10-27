
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class XCH_A_R extends AbstractOpcode
{
	public XCH_A_R(int r)
	{
		super(0xc8|r,1,1,"XCH");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		int tmp = cpu.acc();
		cpu.acc(cpu.r(r));
		cpu.r(r,tmp);
	}

	public String toString()
	{
		return description + "\tA,R" + (opcode & 7);
	}

}

class XCH_A_RI extends AbstractOpcode
{
	public XCH_A_RI(int r)
	{
		super(0xc6|r,1,1,"XCH");
	}

	public void exec(iCPU cpu, int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc(cpu.idata(r));
		cpu.idata(r, tmp);
	}

	public String toString()
	{
		return description + "\tA,@R" + (opcode & 1);
	}

}

class XCHD_A_RI extends AbstractOpcode
{
	public XCHD_A_RI(int r)
	{
		super(0xd6|r,1,1,"XCHD");
	}

	public void exec(iCPU cpu,int pc)
	{
		int r = cpu.r((int)(opcode & 1));
		int tmp = cpu.acc();
		cpu.acc((int)((cpu.acc() & 0xf0) | (cpu.idata(r) & 0x0f)));
		cpu.idata(r,(int)((cpu.idata(r) & 0xf0) | (tmp) & 0x0f));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}


class XCH_A_DIRECT extends AbstractOpcode
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

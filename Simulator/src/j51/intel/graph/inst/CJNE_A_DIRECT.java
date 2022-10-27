
package j51.intel.graph.inst;

import j51.intel.graph.CJNE;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class CJNE_A_DIRECT extends CJNE
{
	public CJNE_A_DIRECT()
	{
		super(0xb5,3,2,"CJNE\tA,DIRECT,#OFFSET");
	}

        @Override
	public void exec(iCPU cpu, int pc)
	{
		cjne(cpu, pc, cpu.acc(), cpu.getDirectCODE(pc + 1), cpu.code(pc + 2));
	}
}

class CJNE_A_DATA extends CJNE
{
	public CJNE_A_DATA()
	{
		super(0xb4,3,2,"CJNE\tA,#DATA8,#OFFSET");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.acc(),cpu.code(pc+1),cpu.code(pc+2));
	}
}
   
class CJNE_R_DATA extends CJNE
{
	public CJNE_R_DATA(int r)
	{
		super(0xb8+r,3,2,"CJNE");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu,pc,cpu.r((int)(opcode & 7)),cpu.code(pc+1),cpu.code(pc+2));
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8,#OFFSET";
	}
}

class CJNE_RI_DATA extends CJNE
{
	public CJNE_RI_DATA(int r)
	{
		super(0xb6+r,3,2,"CJNE");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cjne(cpu, pc, cpu.idata(cpu.r((int)(opcode & 1))), cpu.code(pc + 1), cpu.code(pc + 2));
	}

        @Override
	public String toString()
	{
		return description + "\t@R" + (opcode & 1) + ",#DATA8,#OFFSET";
	}
}

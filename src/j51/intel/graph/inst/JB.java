
package j51.intel.graph.inst;

import j51.intel.graph.JR;
import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JB extends JR
{
	public JB()
	{
		super(0x20,3,2,"JB\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu, int pc)
	{
		if (cpu.getBitCODE(pc + 1))
			jr(cpu, pc, cpu.code(pc + 2));
	}
}

class JBC extends JR
{
	JBC()
	{
		super(0x10,3,2,"JBC\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		if (cpu.getBit(add))
		{
			cpu.setBit(add,false);
			jr(cpu,pc,cpu.code(pc+2));
		}
	}
}

class JNB extends JR
{
	public JNB()
	{
		super(0x30,3,2,"JNB\t#BIT,#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		boolean bit = cpu.getBitCODE(pc+1);

		if (!bit)
			jr(cpu,pc,cpu.code(pc+2));
	}
}

class JNC extends JR
{
	public JNC()
	{
		super(0x50,2,2,"JNC\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (!cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}


class JC extends JR
{
	public JC()
	{
		super(0x40,2,2,"JC\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.cy()){
			jr(cpu,pc,cpu.code(pc+1));
                }
	}
}

class JNZ extends JR
{
	public JNZ()
	{
		super(0x70,2,2,"JNZ\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.acc() != 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}


class JZ extends JR
{
	public JZ()
	{
		super(0x60,2,2,"JZ\t#OFFSET");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		if (cpu.acc() == 0)
			jr(cpu,pc,cpu.code(pc+1));
	}
}


class JMP_A_DPTR extends AbstractOpcode
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

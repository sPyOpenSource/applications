
package j51.intel.graph.inst;

import jCPU.AbstractOpcode;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class MOV_C_BIT extends AbstractOpcode
{
	public MOV_C_BIT()
	{
		super(0xa2,2,1,"MOV\tC,#BIT");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		cpu.cy(cpu.getBitCODE(pc+1));
	}
}

class MOV_BIT_C extends AbstractOpcode
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

class MOV_RI_A extends AbstractOpcode
{
	public MOV_RI_A(int r)
	{
		super(0xf6|r,1,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.r((int)(opcode & 1));
		cpu.idata(add,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}

class MOV_DIRECT_R extends AbstractOpcode
{
	public MOV_DIRECT_R(int r)
	{
		super(0x88|r,2,2,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.r((int)(opcode & 7)));
	}

	public String toString()
	{
		return description+"\tDIRECT,R"+(opcode & 7);
	}

}

class MOV_DIRECT_DATA extends AbstractOpcode
{
	public MOV_DIRECT_DATA()
	{
		super(0x75,3,2,"MOV\tDIRECT,#DATA8");
	}

	public void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.code(pc+2));
	}
}

class MOV_R_DATA extends AbstractOpcode
{
	public MOV_R_DATA(int r)
	{
		super(0x78|r,2,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.r((int)(opcode & 7),cpu.code(pc+1));
	}
	
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#DATA8";
	}

}

class MOV_A_RI extends AbstractOpcode
{
	public MOV_A_RI(int r)
	{
		super(0xe6|r,1,1,"MOV");
	}

	public void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.idata(cpu.r((int)(opcode & 1))));
	}

	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

class MOV_A_DIRECT extends AbstractOpcode
{
	public MOV_A_DIRECT()
	{
		super(0xe5,2,1,"MOV\tA,DIRECT");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.getDirectCODE(pc+1));
	}
}

class MOVC_A_DPTR_A extends AbstractOpcode
{
	public MOVC_A_DPTR_A()
	{
		super(0x93,1,2,"MOVC\tA,@DPTR+A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(cpu.dptr()+cpu.acc()));
	}
}


class MOVX_A_DPTR extends AbstractOpcode
{
	public MOVX_A_DPTR()
	{
		super(0xe0,1,2,"MOVX\tA,@DPTR");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.xdata(cpu.dptr()));
	}
}


class MOVX_DPTR_A extends AbstractOpcode
{
	public MOVX_DPTR_A()
	{
		super(0xf0,1,2,"MOVX\t@DPTR,A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.xdata(cpu.dptr(),cpu.acc());

	}
}


class MOVC_A_PC_A extends AbstractOpcode
{
	public MOVC_A_PC_A()
	{
		super(0x83,1,2,"MOVC\tA,@PC+A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1+cpu.acc()));
	}
}

class MOVX_A_RI extends AbstractOpcode
{
	public MOVX_A_RI(int r)
	{
		super(0xe2|r,1,2,"MOVX");
	}

	public final void exec(iCPU cpu,int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.acc(cpu.xdata(offset));
	}
        
	public String toString()
	{
		return description+"\tA,@R"+(opcode & 1);
	}

}

class MOVX_RI_A extends AbstractOpcode
{
	public MOVX_RI_A(int r)
	{
		super(0xf2|r,1,2,"MOVX");
	}

	public final void exec(iCPU cpu, int pc)
	{
		int offset = cpu.sfr(cpu.getSfrXdataHi()) << 8;
		offset += cpu.idata(cpu.r((int)(opcode & 1)));
		cpu.xdata(offset,cpu.acc());
	}

	public String toString()
	{
		return description+"\t@R"+(opcode & 1)+",A";
	}

}

class MOV_DIRECT_DIRECT extends AbstractOpcode
{
	public MOV_DIRECT_DIRECT()
	{
		super(0x85,3,2,"MOV\tDIRECP,DIRECM");
	}

	public final void exec(iCPU cpu,int pc)
	{
		int source = cpu.code(pc+1);
		int dest = cpu.code(pc+2);
		cpu.setDirect(dest,cpu.getDirect(source));
	}
}

class MOV_A_DATA extends AbstractOpcode
{
	public MOV_A_DATA()
	{
		super(0x74,2,1,"MOV\tA,#DATA8");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.acc(cpu.code(pc+1));
	}
}

class MOV_DIRECT_A extends AbstractOpcode
{
	public MOV_DIRECT_A()
	{
		super(0xf5,2,1,"MOV\tDIRECT,A");
	}

	public final void exec(iCPU cpu,int pc)
	{
		cpu.setDirect(cpu.code(pc+1),cpu.acc());
	}
}

class MOV_DIRECT_RI extends AbstractOpcode
{
	public MOV_DIRECT_RI(int r)
	{
		super(0x86|r,2,2,"MOV");
	}

        @Override
	public final void exec(iCPU cpu,int pc)
	{
		int add = cpu.code(pc+1);
		cpu.setDirect(add,cpu.idata(cpu.r((int)(opcode & 1))));
	}

        @Override
	public String toString()
	{
		return description+"\tDIRECT,@R"+(opcode & 1);
	}
}


package jCPU.MCS51.graph.inst;

import jCPU.MCS51.CPU;
import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class DJNZ_R extends DJNZ
{
    
	public DJNZ_R(int r)
	{
		super(0xd8|r,2,2,"DJNZ");
	}

        @Override
	public void exec(iCPU cpu,int pc)
	{
		int r = (int)(opcode & 7);
		int value = (int)(cpu.r(r) - 1);
		cpu.r(r,value);
		jnz((CPU) cpu,pc,value);
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#OFFSET";
	}

}
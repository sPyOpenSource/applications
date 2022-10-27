
package j51.intel.graph.inst;

import j51.intel.MCS51;
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
		jnz((MCS51) cpu,pc,value);
	}

        @Override
	public String toString()
	{
		return description+"\tR"+(opcode & 7)+",#OFFSET";
	}

}

package j51.intel.graph;

import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public abstract class CJNE extends JR
{
    
	public CJNE(int opcode,int len,int cycle,String desc)
	{
		super(opcode,len,cycle,desc);
	}

	protected final void cjne(iCPU cpu,int pc,int op1,int op2,int offset)
	{
		
		if (op1 < op2)
		{
			cpu.cy(true);
		} else {
			cpu.cy(false);
		}

		if (op1 != op2){
			jr(cpu,pc,offset);
                }
	}

}

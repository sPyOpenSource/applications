
package j51.swing;

import jCPU.iCPU;

/**
 *
 * @author xuyi
 */
public class JXdata extends JData
{
    
	public JXdata()
	{
		super("Xdata",0,0x10000);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.xdata(address);
	}

        @Override
	public void setCpu(iCPU cpu)
	{
		super.setCpu(cpu);
		setTop(cpu.getXdataSize());
	}

}

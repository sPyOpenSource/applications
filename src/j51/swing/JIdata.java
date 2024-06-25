
package j51.swing;

/**
 *
 * @author xuyi
 */
public class JIdata extends JData
{
    
	public JIdata()
	{
		super("Idata",0,256);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null){
			return 0;
                }

		return cpu.idata(address);
	}

}

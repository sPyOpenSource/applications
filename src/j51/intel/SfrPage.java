/**
 * $Id: SfrPage.java 70 2010-07-01 09:57:00Z mviara $
 */
package j51.intel;

/**
 * SFR memory.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * @since 1.04
 *
 * 1.01 Added support for copy SFR from different page.
 */
public class SfrPage extends VolatileMemory
{
	private int page;
	
	public SfrPage(int page)
	{
		super("SFR", 256);
		this.page = page;
		
		for (int i = 0 ; i < 256 ; i++)
			setMemory(i, new SfrRegister(i));
		setPresent(0, 256);
	}

	public SfrPage(int page, SfrPage base)
	{
		super("SFR#" + page, 256);
		this.page = page;
		for (int i = 0 ; i < 256 ; i++)
			setMemory(i, base.getMemory(i));
	}
	
	public SfrRegister getReg(int i)
	{
		return (SfrRegister)getMemory(i);
	}
}

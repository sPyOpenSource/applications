/**
 * $Id: LPC936.java 46 2010-06-22 21:04:52Z mviara $
 */

package j51.philips;

/**
 *
 * NXP LPC936 like NXP LPC900 family but with 16 KB flash memory.
 *
 * @author Mario Viara
 * @version 1.00
 * @since 1.04
 * 
 */
public class LPC936 extends LPC900
{
	public LPC936() throws Exception
	{
		this("LPC936");
	}

	public LPC936(String name) throws Exception
	{
		super(name,16*1024);
	}
	
		     
}

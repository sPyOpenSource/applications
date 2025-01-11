/**
 * $Id: LPC764.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.philips;


/**
 *
 * LPC764 Emulator emulated peripheral :
 * 
 * I/O port
 * 
 * @author Mario Viara
 * @version 1.00
 *
 */
public class LPC764 extends LPC764Base
{
	
	public LPC764() throws Exception
	{
		super("LPC764");
		
		addPeripheral(new LPC764Ports());


	}

	public String toString()
	{
		return "Philips 87LPC764 $Id: LPC764.java 56 2010-06-24 20:06:35Z mviara $";
	}
}

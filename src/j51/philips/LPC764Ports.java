/**
 * $Id: LPC764Ports.java 48 2010-06-23 08:28:23Z mviara $
 */

package j51.philips;

import j51.intel.*;



/**
 * LPC764 Port
 *
 * @author Mario Viara
 * @version 1.00
 */
public class LPC764Ports extends JPort 
{
	public LPC764Ports() throws Exception
	{
		super(3);
		setDisableMask(2,0xFC);
	}
}

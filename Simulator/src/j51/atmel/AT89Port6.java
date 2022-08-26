/**
 * $Id: AT89Port6.java 49 2010-06-23 08:38:02Z mviara $
 */
package j51.atmel;

import j51.intel.*;



/**
 *
 * Atmel 89C51Rxx port
 * 
 * @author Mario Viara
 * @version 1.00
 */
public class AT89Port6 extends JPort implements AT89C51RD2Constants
{
	public AT89Port6()
	{
		super(6);
		setSfrP(4,P4);
		setSfrP(5,P5);
	}
}

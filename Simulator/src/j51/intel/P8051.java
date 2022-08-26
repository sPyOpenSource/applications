/**
 * $Id: P8051.java 45 2010-06-22 20:53:26Z mviara $
 *
 */
package j51.intel;

/**
 *
 * Standard 8051 microprocessor. With all peripheral.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class P8051 extends MCS51
{
	public P8051() throws Exception
	{
		addPeripheral(new Timer());
		addPeripheral(new JPort(4));
		addPeripheral(new JUartTA());
	}

	public String toString()
	{
		return "Intel 8051  $Id: P8051.java 45 2010-06-22 20:53:26Z mviara $";
	}
}

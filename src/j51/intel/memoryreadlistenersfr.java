/**
 * $Id: MemoryReadListenerSfr.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;


/**
 * Utility class to convert old SfrReadListener in MemoryReadListener.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
class MemoryReadListenerSfr implements MemoryReadListener
{
	SfrReadListener l;

	MemoryReadListenerSfr(SfrReadListener l)
	{
		this.l = l;
	}

	public int readMemory(int address,int value)
	{
		return l.sfrRead(address);
	}

}



/**
 * $Id: MemoryWriteListenerSfr.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

import jCPU.MemoryWriteListener;

/**
 * Utility class to convert old SfrWriteListener in MemoryWriteListener.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public class MemoryWriteListenerSfr implements MemoryWriteListener
{
	SfrWriteListener l;

	MemoryWriteListenerSfr(SfrWriteListener l)
	{
		this.l = l;
	}

	public void writeMemory(int address,int newValue,int oldValue)
	{
		l.sfrWrite(address,newValue);
	}

}


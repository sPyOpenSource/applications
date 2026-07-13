/**
 * $Id: AsyncTimerListener.java 46 2010-06-22 21:04:52Z mviara $
 */
package jCPU;

import jCPU.MCS51.CPU;

/**
 *
 * Interface to call one asyncronous timer
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface AsyncTimerListener
{
	public void expired(CPU cpu) throws Exception;
}

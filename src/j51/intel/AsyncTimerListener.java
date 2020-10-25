/**
 * $Id: AsyncTimerListener.java 46 2010-06-22 21:04:52Z mviara $
 */
package j51.intel;

/**
 *
 * Interface to call one asyncronous timer
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface AsyncTimerListener
{
	public void expired(MCS51 cpu) throws Exception;
}

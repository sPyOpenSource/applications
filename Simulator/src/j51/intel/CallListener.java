/**
 * $Id: CallListener.java 46 2010-06-22 21:04:52Z mviara $
 */
package j51.intel;

/**
 *
 * Interface to intercept one call.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface CallListener
{
	public void call(MCS51 cpu,int pc) throws Exception;
}

/**
 * $Id: MachineCyclesListener.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/**
 * This listener must be implemented from the peripheral that must
 * 'polling' some event. The interface will be called from the cpu
 * after any instruction.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface MachineCyclesListener
{
	/**
	 * Number of cpu cycles elapsed
	 */
	public void cycles(int cycles);
}
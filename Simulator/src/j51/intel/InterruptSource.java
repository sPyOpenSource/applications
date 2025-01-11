/**
 * $Id: InterruptSource.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/**
 *
 * Interface for implements one interrupt source, this interface is
 * used when a SFR coinvolted is written.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface InterruptSource
{
	/**
	 * Return true if the interrupt condition is true
	 */
	public boolean		interruptCondition();

	/**
	 * Return the address of the interrupt service.
	 */
	public int		getInterruptVector();

	/**
	 * Called before to execute the interrupt service.
	 */
	public void		interruptStart();

	/**
	 * Called after the interrupt service is terminated.
	 */
	public void		interruptStop();

}

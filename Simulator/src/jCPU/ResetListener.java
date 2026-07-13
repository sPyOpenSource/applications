/**
 * $Id: ResetListener.java 45 2010-06-22 20:53:26Z mviara $
 */
package jCPU;

import jCPU.MCS51.CPU;

/**
 * Interface for listener waiting reset cycle of the main cpu.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface ResetListener
{
	/**
	 * Called when the main cpu is reset.
	 */
	public void reset(CPU cpu);
}

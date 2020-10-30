/**
 * $Id: MemoryReadListener.java 56 2010-06-24 20:06:35Z mviara $
 */
package jCPU;

/**
 * Listener to trace read in memory.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public interface MemoryReadListener
{
	public int readMemory(int address,int value);
};


/**
 * $Id: MemoryWriteListener.java 56 2010-06-24 20:06:35Z mviara $
 */
package jCPU;

/**
 * Listener to trace write in memory.
 * 
 * @author Mario Viara
 * @version 1.00
 *
 * @sice 1.04
 */
public interface MemoryWriteListener
{
	public void writeMemory(int address, int newValue, int oldValue);
}

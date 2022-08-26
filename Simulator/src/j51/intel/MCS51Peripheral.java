/**
 * $Id: MCS51Peripheral.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/**
 * Interface for MCS51 peripheral
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface MCS51Peripheral
{
	/**
	 * Register the cpu to the peripheral
	 */
	public void registerCpu(MCS51 cpu);

}


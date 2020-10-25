/*
 * $Id: EmulationListener.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.intel;

/**
 * Interface to be notified when emulation start / stop.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public interface EmulationListener
{
	public void setEmulation(boolean mode);
}

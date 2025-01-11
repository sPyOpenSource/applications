/**
 * $Id: SfrReadListener.java 56 2010-06-24 20:06:35Z mviara $
 */
package j51.intel;

/**
 * Interface to read one SFR register. 
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface SfrReadListener
{
	public int sfrRead(int r);
}

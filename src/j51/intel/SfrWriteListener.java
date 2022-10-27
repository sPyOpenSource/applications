/*
 * $Id: SfrWriteListener.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/*
 * Interface for write a SFR register. Many peripheral can implements
 * this interface.
 * 
 * @author Mario Viara
 * @version 1.00
 */
public interface SfrWriteListener
{
	public void sfrWrite(int r,int v);
}

/**
 * $Id: Motor.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.philips.diseqc;

import java.awt.*;

/**
 * 
 * Interface to rappresent a DISEQC Motor.
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public interface Motor
{
	public void setPosition( int pos);
	public Component getComponent();
}

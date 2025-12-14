/**
 * $Id: JFactory.java 63 2010-06-30 06:24:49Z mviara $
 */

package j51.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 *
 * Swing factory used to have one common 'skin' in the J51 swing
 * omponent.
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public class JFactory
{
    
	public static void setBox(JComponent j)
	{
		j.setBorder(BorderFactory.createEtchedBorder());

	}
	
	public static void setTitle(JComponent j,String title)
	{
		Border etched = BorderFactory.createEtchedBorder();
		Border titled = BorderFactory.createTitledBorder(etched, " " + title + " ");
		titled = BorderFactory.createTitledBorder(" "+title+" ");
		j.setBorder(titled);
	}

	public static Color getColorNormal()
	{
		return Color.blue;
	}

	public static Color getColorSelected()
	{
		return Color.red;
	}
	
}

/**
 * $Id: JLed.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.swing;

import java.awt.*;
import javax.swing.*;

/**
 * 
 * Simple class to implement a software led.
 * This implementation use a JTextField filled with space and change
 * the background color when the led is on or off.
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public class JLed extends JPanel
{

	private JTextField led ;
	private boolean mode = true;
	private Color color;

	public JLed(String label,Color color)
	{
		this(label,color,6);
	}
	public JLed(String label,Color color,int size)
	{
		led = new JTextField(size);
		JFactory.setTitle(this,label);
		add(led);
		set(false);
		led.setEditable(false);
		this.color = color;
	}

	public JLed(String label)
	{
		this(label,Color.red);
	}

	public void set(boolean mode)
	{
		if (mode != this.mode)
		{
			this.mode = mode;
			led.setBackground(mode ? color : color.black);
		}
	}

	public void set()
	{
		set(true);
	}

	public void reset()
	{
		set(false);
	}
	
	public boolean get()
	{
		return mode;
	}

	public void on()
	{
		set(false);
	}

	public void off()
	{
		set(true);
	}

}

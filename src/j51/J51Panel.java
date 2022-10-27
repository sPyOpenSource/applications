/**
 * $Id: J51Panel.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51;

import java.awt.*;
import javax.swing.*;
import j51.intel.*;
import j51.swing.JFactory;

/**
 *
 * 8051 Simulator base panel.
 *  
 * @author Mario Viara
 * @version 1.00
 */
public class J51Panel extends JPanel
{
	protected MCS51 cpu;
	private String title;

	public J51Panel(String title)
	{
		this(title, true);
	}
	
	public J51Panel(String title, boolean box)
	{
		super(new GridBagLayout());
		this.title = title;
		if (box)
		{
			if (title == null){
				JFactory.setBox(this);
                        } else {
				JFactory.setTitle(this, title);
                                        }
		}
	}

	public String getTitle()
	{
		return title;
	}

	public void setCpu(MCS51 cpu)
	{
		this.cpu = cpu;
	}

	public void setEmulation(boolean mode)
	{
	}

	public void update(boolean force)
	{
	}
}

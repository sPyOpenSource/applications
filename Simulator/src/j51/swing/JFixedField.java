/**
 * $Id: JFixedField.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Special implementation of JTextField with fixed lenght.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class JFixedField extends JTextField implements FocusListener
{
	protected int len;
	private boolean selected = false;

	public JFixedField(int len)
	{
		this(len,false);
	}
	
	public JFixedField(int len,boolean bold)
	{
		this.len = len;
		Font font = getFont();
		int style = font.getStyle();
		if (bold)
			style = Font.BOLD;
		font = new Font("Monospaced",style,font.getSize());
		setEditable(false);
		setFont(font);
		setSelected(false);
		setText("");
		addFocusListener(this);
	}

	public void setText(String s)
	{

		while (s.length() < len)
		{
			if (getHorizontalAlignment() == RIGHT)
				s = " "+s;
			else
				s += " ";
		}
		s = s.substring(0,len);

		String old = getText();

		if (old == null)
			old = "";
		if (!s.equals(old))
			super.setText(s);
	}

	public void setSelected(boolean selected)
	{
		this.selected = selected;

		setForeground(selected ? JFactory.getColorSelected() : JFactory.getColorNormal());
	}

	public boolean getSelected()
	{
		return selected;
	}

	public void setEditable(boolean mode)
	{
		super.setEditable(mode);
		setFocusable(mode);
	}
	

	public void focusLost(FocusEvent e)
	{
	}

	public void focusGained(FocusEvent e)
	{
		if (isEditable() && isEnabled())
			selectAll();
	}

}

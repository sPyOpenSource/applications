/**
 * $Id: JNumField.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.swing;

import java.awt.event.*;
import javax.swing.text.*;


class NumTextDocument extends PlainDocument
{
	private int size;

	NumTextDocument(int size)
	{
		this.size = size;
	}

	public void insertString(int offset,String string,AttributeSet a)
			throws BadLocationException
	{
		if (string != null)
		{
			if (getLength() + string.length() > size)
			{
				//Toolkit.getDefaultToolkit().beep();
				return;
			}
		}
		else
			return;

		StringBuffer s = new StringBuffer(string);
		boolean isValid = true;

		for (int i = 0 ; i < s.length() && isValid ; i++)
		{
			char c = s.charAt(i);


			if	(!Character.isDigit(c))
			{
				if (c != ' ')
					isValid = false;
			}

			s.setCharAt(i,c);
		}

		if (isValid == false)
		{
			//Toolkit.getDefaultToolkit().beep();
			return;
		}

		super.insertString(offset,s.toString(),a);
	}


}

/**
 * Class to edit a field in decimal format.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class JNumField extends JFixedField  implements KeyListener
{
	public JNumField(int len)
	{
		this(len,false);
	}
	
	public JNumField(int len,boolean bold)
	{
		super(len,bold);
		setDocument(new NumTextDocument(len));
		setHorizontalAlignment(RIGHT);
		setValue(0);
		addKeyListener(this);
	}

	public long getValue() 
	{
		String s = getText();
		long value = 0;

		try
		{
			value = Long.parseLong(s);
		}
		catch (Exception e)
		{
		}
		
		return value;
	}

	public void setValue(long value)
	{
		String s = "0";
		long old = 0;

		try
		{
			s = Long.toString(value);
		}
		catch (Exception e)
		{
		}

		try
		{
			old = getValue();
		} catch (Exception e)
		{
		}


		setText(s);

		if (value == old)
		{
			if (getSelected())
				setSelected(false);
		}
		else
		{
			setSelected(true);
		}

	}

	public void keyPressed(KeyEvent e)
	{
		key(e);
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	private void key(KeyEvent e)
	{
		int offset = 0;

		if (isEditable())
		{

			switch (e.getKeyCode())
			{
				case	KeyEvent.VK_PAGE_UP:
						offset = 16;
						break;
				case	KeyEvent.VK_PAGE_DOWN:
						offset = -16;
						break;
				case	KeyEvent.VK_UP:
						offset = 1;
						break;
				case	KeyEvent.VK_DOWN:
						offset = -1;
						break;
			}

			if (offset != 0)
			{
				try
				{
					long value = getValue();
					setValue((value+offset) & 0xffff);
				}
				catch (Exception ex)
				{
				}
			}
		}

		if (offset != 0)
		{
			e.setKeyCode(e.VK_ENTER);
		}
	}

}

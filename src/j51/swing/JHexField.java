/**
 * $Id: JHexField.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.swing;

import java.awt.event.*;
import javax.swing.text.*;

import j51.util.Hex;


class HexTextDocument extends PlainDocument
{
	private int size;

	HexTextDocument(int size)
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

			/**
			 * Convert char in uppercase
			 */
			if (Character.isLowerCase(c))
				c = Character.toUpperCase(c);


			if	(!Character.isDigit(c))
			{
				if (c < 'A' || c > 'F')
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
 * Class to edit a field in hexadecimal.
 * 
 * @author Mario Viara
 * @version 1.00
 */
public class JHexField extends JFixedField  implements KeyListener
{
	JHexField(int len)
	{
		this(len,false);
	}
	
	JHexField(int len,boolean bold)
	{
		super(len,bold);
		setDocument(new HexTextDocument(len));
		setHorizontalAlignment(RIGHT);
		setValue(0);
		addKeyListener(this);
	}

	public int getValue() throws Exception
	{
		String s = getText();
		int value = 0;

		try
		{
			for (int i = 0 ; i < s.length() ; i++)
			{
				value = (value <<4) | Hex.getDigit(s,i);
			}
			setValue(value);
		}
		catch (Exception e)
		{
			setText("0");
		}
		return value;
	}

	public void setValue(int value)
	{
		String s;

		if (len == 4)
		{
			s = Hex.bin2word(value);
		}
		else
			s = Hex.bin2byte(value);

		String old = getText();

		if (old == null)
			old = "";

		if (s.equals(old))
		{
			if (getSelected())
				setSelected(false);
		}
		else
		{
			setSelected(true);
			setText(s);
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
					int value = getValue();
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
			//e.consume();
			//fireActionPerformed();
		}
	}

}

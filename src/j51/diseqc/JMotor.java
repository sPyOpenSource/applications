/**
 * $Id: JMotor.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.diseqc;

import java.awt.*;
import javax.swing.*;

import j51.swing.*;

/**
 * Sample text implementation of Motor.
 *
 * @author Mario Viara
 * @version 1.00
 *
 * @deprecated
 */
public class JMotor extends JPanel implements Runnable, Motor
{
	int value = -1;
	String stringValue;
	JTextField f;
	JMotor(int min, int max, int value)
	{
		f = new JTextField();
		Font font = f.getFont();
		font = new Font("Monospaced",Font.BOLD,font.getSize()*4);
		f.setFont(font);
		f.setForeground(Color.green);
		f.setBackground(Color.black);
		f.setEditable(false);
		setPosition(0);
		add(f);
		JFactory.setTitle(this,"Position");
	}

        @Override
	public void setPosition(int value)
	{
		value /= 10;


		if (value == this.value)
			return;
		this.value = value;
		String s;

		if (value == 0)
			s = "  000 ";
		else
		{
			String p;
			if (value < 0)
			{
				p = "E";
				value = - value;
			}
			else
			{
				p = "W";
			}
			s = value+p;
		}

		while (s.length() < 5)
			s = " "+s;

		stringValue = s;

		SwingUtilities.invokeLater(this);

	}

        @Override
	public Component getComponent()
	{
		return this;
	}

        @Override
	public void run()
	{
		f.setText(stringValue);
	}
}

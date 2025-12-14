/**
 * $Id: JUart.java 48 2010-06-23 08:28:23Z mviara $
 */
package j51.swing;

import j51.intel.MCS51;
import j51.device.Uart;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 *
 * Standard 8051 uart implementation, no baud rate are used but only
 * the send and receive char from the SBUF register.
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public class JUart extends Uart implements KeyListener
{
	private JTextArea textArea = new JTextArea(1000,80);
	
	public JUart()
	{
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		screen.height /= 4;
		screen.width  /= 4;

		textArea.setLineWrap(true);
		setPreferredSize(screen);
		
		textArea.setForeground(Color.green);
		textArea.setBackground(Color.black);
		
		add(new JScrollPane(textArea,
			  JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			  JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),BorderLayout.CENTER);
		Font font = textArea.getFont();
		font = new Font("Monospaced",Font.PLAIN,font.getSize());
		textArea.setFont(font);
		textArea.addKeyListener(this);
	}

	public void keyTyped(KeyEvent e)
	{
		e.consume();
	}

	public void keyPressed(KeyEvent e)
	{
		char c = e.getKeyChar();

		/**
		 * Convert VK_ENTER,CTRL-J,CTRL-M in CR
		 */
		if (e.getKeyCode() == e.VK_ENTER)
			c = 13;

		if (c == 10)
			c = 13;
		
		if (c != e.CHAR_UNDEFINED)
		{
			recvChar(c);
		}
		e.consume();
	}

        @Override
	public void keyReleased(KeyEvent e)
	{
		e.consume();
	}

        @Override
	public void reset(MCS51 cpu)
	{
		super.reset(cpu);
		textArea.setText("");
	}

        @Override
	protected void sendChar(int v)
	{
		byte b[] = new byte[1];
		b[0] = (byte)v;
		if ((v < 32 || v >= 127) && v != 13 && v != 10 && v != 9 && v != 8)
		{
			String s = "<"+j51.util.Hex.bin2byte(v)+">";
			textArea.append(s);
		}
		else
			textArea.append(new String(b));
					
		textArea.setCaretPosition(textArea.getDocument().getLength());

		// Keep the text area down to a certain character size
		int idealSize = 20 * 1024;
		int maxExcess = 500;
		int excess = textArea.getDocument().getLength() - idealSize;
		if (excess >= maxExcess) 
			textArea.replaceRange("", 0, excess);
	}

}

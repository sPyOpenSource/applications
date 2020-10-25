/**
 * $Id: JBitField.java 62 2010-06-29 22:06:12Z mviara $
 */
package j51.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;

/**
 * 
 * CheckBox used to implements I/O port.
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public class JBitField extends JPanel implements ActionListener,UpdatableComponent
{
	private JCheckBox bits[] = new JCheckBox[8];
	private Vector listeners = new Vector();
	private int oldValue = -1,value;
	private static Vector fields = null;
	
	public JBitField(String title)
	{
		this(title,true);
	}
	
	public JBitField(String title,boolean bit)
	{
		super(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = g.CENTER; g.fill = g.NONE; 

		for (int i = 0  ; i < 8 ; i ++)
		{
			String label = ""+(7-i);
			g.gridy = 0;
			
			if (bit)
			{
				add(new JLabel(label),g);
				g.gridy = 1;
			}
			
			bits[7-i] = new JCheckBox();
			bits[7-i].setMargin(new Insets(0,0,0,0));
			bits[7-i].addActionListener(this);
			bits[7-i].setToolTipText(label);
			add(bits[7-i],g);
			g.gridx++;
		}

		JFactory.setTitle(this,title);
		setValue(0);


	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		for (int i = 0  ;i < listeners.size() ; i++)
		{
			ActionListener l = (ActionListener)listeners.elementAt(i);
			l.actionPerformed(e);
		}
		
	}

	public void setBitName(int bit,String name)
	{
		bits[bit].setToolTipText(name);

	}

	public void setDisabled(int bit,boolean mode)
	{
		bits[bit].setEnabled(!mode);
	}
	
	public void setValue(int value)
	{
		this.value = value;
		
	}

	public boolean update()
	{
		if (value == oldValue)
			return false;
		
		for (int i = 0  ; i < 8 ; i ++)
		{
			boolean oldBit = bits[i].isSelected();
			boolean newBit = (value & (1 << i)) != 0;

			if (oldBit != newBit)
				bits[i].setSelected(newBit);
		}

		oldValue = value;

		return true;
	}
	
	
	public int getValue()
	{
		int value = 0;
		
		for (int i = 0  ; i < 8 ; i ++)
		{
			if (bits[i].isSelected())
				value |= 1 << i;
		}

		return value;
	}

	public void setEditable(boolean mode)
	{
		for (int i = 0  ; i < 8 ; i ++)
		{
			bits[i].setEnabled(mode);
		}
	}

	public void addActionListener(ActionListener l)
	{
		listeners.add(l);
	}
	
	
}

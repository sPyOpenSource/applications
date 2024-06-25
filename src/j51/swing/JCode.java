
package j51.swing;

import j51.intel.MCS51;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author xuyi
 */
public class JCode extends JData
{
	public JCode()
	{
		super("Code", 0, 0x10000);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.code(address);
	}

        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		setTop(cpu.getCodeSize());
	}
	
}

class Rect extends JComponent
{
	private Color color;
	private final Dimension size;


	Rect(Color color,int w,int h)
	{
		this.color = color;
		size = new Dimension(w,h);
		setPreferredSize(size);
	}

        @Override
	public void paintComponent(Graphics g)
	{
		g.setColor(color);
		g.fillRect(0,0,size.width,size.height);
	}

	public void setColor(Color color)
	{
		this.color = color;
		repaint();
	}

}

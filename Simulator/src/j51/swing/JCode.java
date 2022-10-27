
package j51.swing;

import j51.J51Panel;
import j51.intel.MCS51;
import j51.intel.MCS51Peripheral;
import j51.util.Hex;

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
	JCode()
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

class JXdata extends JData
{
	JXdata()
	{
		super("Xdata",0,0x10000);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.xdata(address);
	}

        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		setTop(cpu.getXdataSize());
	}

}

class JIdata extends JData
{
	JIdata()
	{
		super("Idata",0,256);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null){
			return 0;
                }

		return cpu.idata(address);
	}

}

class JSfr extends J51Panel
{
	JLabel labels[] = new JLabel[128];
	JHexByte fields[] = new JHexByte[128];
	
	JSfr()
	{
		super("JSFR", false);
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0 ; g.gridy = 0;g.gridwidth = 1;g.gridheight=1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(1,1,1,1);

		for (int i = 0 ; i < 128 ; i++)
		{
			labels[i] = new JLabel(Hex.bin2byte(i + 128) + "  ");
			fields[i] = new JHexByte();
			fields[i].setEditable(false);
			fields[i].setToolTipText("Address " + Hex.bin2byte(i + 128));
			add(labels[i],g);
                        g.gridx++;
			add(fields[i],g);
                        g.gridx++;
			if (g.gridx == 16)
			{
				g.gridy++;
				g.gridx = 0;
			}
		}
		
	}
	
        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		for (int i = 0 ; i < 128 ; i++)
		{
			String s= cpu.getSfrName(i + 128);
			labels[i].setToolTipText(s);

			if (s.length() > 4){
				s= s.substring(0, 4);
                        }
			labels[i].setText(s);
		}
	}

        @Override
	public void update(boolean force)
	{
		for (int i = 0 ; i < 128 ; i++){
			fields[i].setValue(cpu.sfr(i + 128));
                }
	}
	
}

class JPeripheral extends J51Panel
{
	JPeripheral()
	{
		super("Peripheral");
	}

	
        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		removeAll();
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; 
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; 
                g.fill = GridBagConstraints.BOTH;
		for (int i = 0 ; i < cpu.getPeripheralsCount() ; i++)
		{
			MCS51Peripheral p = cpu.getPeripheralAt(i);
			
			if (p instanceof Component)
			{
				add((Component)p, g);
				g.gridy++;
			}
		}
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


class Worker extends JDialog implements Runnable
{
	private Object result = null;
	private final Rect rectangle[];
	private javax.swing.Timer timer;
	private int paintIndex = 0;
	private Color paintColor = Color.blue;
	private final JLabel progress;
	
	Worker(JFrame parent, String msg1, String msg2)
	{
		super(parent, true);

		setTitle(msg1);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;
		g.fill  = GridBagConstraints.NONE;
                g.insets = new Insets(5, 5, 5, 5);
		g.anchor = GridBagConstraints.CENTER;

		g.gridwidth = 2;
		p.add(new JLabel(msg1), g);
		g.gridwidth = 1; 
                g.gridy++;
		//p.add(new JLabel(Utils.getIcon(Utils.ICON_PROCESS24)),g);
		g.gridx++;
		progress = new JLabel(msg2);
		p.add(progress,g);

		//setIconImage(Utils.getIcon(Utils.ICON_PROCESS).getImage());
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE );

		JPanel p1 = new JPanel(new GridBagLayout());
		GridBagConstraints g1 = new GridBagConstraints();
		g1.gridx = 0;g1.gridy = 0;g1.gridwidth = 1;g1.gridheight = 1;
		g1.fill  = GridBagConstraints.NONE;g1.insets = new Insets(1,1,1,1);
		g1.anchor = GridBagConstraints.CENTER;

		rectangle = new Rect[10];
		for (int i = 0 ; i < rectangle.length ; i++)
		{
			rectangle[i] = new Rect(Color.gray,20,15);
			p1.add(rectangle[i],g1);
			g1.gridx++;
		}
		g.gridx = 0; g.gridy++;g.gridwidth = 2;
		p.add(p1,g);
		setContentPane(p);
		pack();
		setLocationCenter(this);
	}

	public void setProgress(String msg)
	{
		progress.setText(msg);
	}
	
        @Override
	public void run()
	{
		try
		{
			process();
		} catch (Exception ex) {
		}

		timer.stop();

		setVisible(false);
		SwingUtilities.invokeLater(() -> {
                    swingProcess();
                });
	}

	public void setResult(Object r)
	{
		result = r;
	}

	public Object getResult()
	{
		return result;
	}

	public void process()
	{
	}

	public void swingProcess()
	{
	}

	void paintRectangle()
	{
		rectangle[paintIndex].setColor(paintColor);

		if (++paintIndex >= rectangle.length)
		{
			paintColor = paintColor ==
				     Color.blue ? Color.gray :
				     Color.blue;
			paintIndex = 0;
		}
	}

	void start()
	{
		Thread t = new Thread(this);
		t.start();
		timer = new javax.swing.Timer(100, (ActionEvent e) -> {
                    paintRectangle();
                });
		timer.start();
		setVisible(true);
	}

	static public void setLocationCenter(Component target)
	{
		Container parent = target.getParent();

		if (parent.isShowing() == false){
			parent = null;
                }

		if (parent == null){
			Dimension dim = target.getToolkit().getScreenSize();
			Rectangle abounds = target.getBounds();
			target.setLocation((dim.width - abounds.width) / 2,
					   (dim.height - abounds.height) / 2);
		} else {
			int x;
			int y;

			Point topLeft = parent.getLocationOnScreen();
			Dimension parentSize = parent.getSize();

			Dimension mySize = target.getSize();

			if (parentSize.width > mySize.width) {
				x = ((parentSize.width - mySize.width) / 2) + topLeft.x;
                        } else {
				x = topLeft.x;
                        }

			if (parentSize.height > mySize.height) {
				y = ((parentSize.height - mySize.height) / 2) + topLeft.y;
                        } else {
				y = topLeft.y;
                        }

			target.setLocation (x, y);

		}

	}

}

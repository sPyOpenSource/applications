/**
 * $Id: Satellite.java 69 2010-07-01 07:00:39Z mviara $
 */
package j51.philips.diseqc;

import java.awt.*;
import javax.swing.*;
import java.net.*;


/**
 * Satellite implementation of motor.
 *
 * @author Mario Viara
 * @version 1.00
 * 
 * @since 1.04
 */
public class Satellite extends JComponent implements Motor
{
	private String sPos = "";
	private final Font font = new Font("Monospaced",Font.BOLD,20);
	private final ImageIcon sat;
	private int pos;
	private long time = 0;
	
	public Satellite()
	{
		int w = 200;
		int h = 80;
		sat = getIcon("satellite.gif");
		Dimension dim = new Dimension(w,h);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setPreferredSize(dim);
		setSize(dim);
		setPosition(0);
	}

        @Override
	public void setPosition(int pos)
	{
		this.pos = pos;
		Double d = pos / 10.0;
		
		if (d == 0)
			sPos = "00.0";
		else if (pos < 0)
			sPos = -d+"E";
		else
			sPos =  d+"W";

		while (sPos.length() < 5)
			sPos = " "+sPos;

		long now = System.currentTimeMillis();
		if (now > time)
		{
			time = now  + 100;
				
			repaint();
		}
	}

        @Override
	public void paint(Graphics g)
	{
		int x,y,w;
		Dimension d = getSize();
		g.setColor(Color.black);
		g.fillRect(0,0,d.width,d.height);

		if (sat != null)
		{
			w = d.width - 36;
			y = pos / 30;
			if (y < 0)
				y = -y;
			
			if (pos < 0)
				pos = 900 - pos;
			else
				pos = 900 - pos;
			
			
			x = pos * w / 1800;
			g.drawImage(sat.getImage(),x,6+y,sat.getImageObserver());
				
		}

		g.setColor(Color.red);
		g.setFont(font);

		FontMetrics fm = g.getFontMetrics();
		x = (d.width - fm.charWidth('W') * sPos.length()) / 2;
		y =  d.height - 3;
		g.drawString(sPos,x,y);
	}

	/**
	 * Load one icon from the resource.
	 *
	 * @sice 1.04
	 */
	public ImageIcon getIcon(String name)
	{
		URL url = Satellite.class.getResource("images/" + name);

		if (url == null){
			return null;
                }

		ImageIcon  icon =  new ImageIcon(url);

		return icon;


	}

	public Component getComponent()
	{
		return this;
	}


	static public void main(String argv[])
	{
		final Satellite s = new Satellite();
		JFrame f = new JFrame("Test Satellite");
		f.setContentPane(s);
		f.pack();
		f.setVisible(true);

		Thread t = new Thread(() -> {
                    for (int i = 900; i >= -900 ; i -= 9)
                    {
                        s.setPosition(i);
                        try
                        {
                            Thread.sleep(100);
                        } catch (InterruptedException ignore) {
                        }
                    }
                });

		t.start();
			
	}

}

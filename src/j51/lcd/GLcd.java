/**
 * $Id: GLcd.java 48 2010-06-23 08:28:23Z mviara $
 */
package j51.lcd;

import java.awt.*;
import javax.swing.*;


/**
 * Graphics LCD display adapter.
 *
 * @author Mario Viara
 * @version 1.00
 */
public class GLcd extends JComponent
{
	private final int pixelSize;
	private final int width;
	private final int height;
	private final Color colorBack  = new Color(0,0,0);
	private final Color colorFront = new Color(0,255,0);
	private final Dimension size;
	private final byte memory[];
	private final java.util.HashMap patterns = new java.util.HashMap();

	public GLcd(int width,int height,int pixelSize)
	{
		this.width = width;
		this.height = height;
		this.pixelSize = pixelSize;
		size = new Dimension(width * pixelSize,height * pixelSize);
		setPreferredSize(size);
		memory = new byte[width / 8 * height];
		for (int i = 0 ; i < memory.length ; i++)
			memory[i ] = 0;
	}

        @Override
	public void paintComponent(Graphics g)
	{
		Insets insets = getInsets();

		g.translate(insets.left,insets.top);

		for (int x = 0 ; x < width ; x += 8)
			for (int y = 0; y < height ; y++)
				g.drawImage(getImage(x,y),pixelSize*x,pixelSize*y,null);

	}

	public byte getMemory(int address)
	{
		return memory[address];
	}

	public void setMemory(int address,byte b)
	{
		if (memory[address] != b)
		{
			memory[address] = b;
			repaint();
		}
	}
	
	public Image getImage(int x, int y)
	{
		int p = memory[y * width / 8 + x / 8];
		Image img = (Image)patterns.get(p);

		if (img == null)
		{
			img = createImage(8*pixelSize,pixelSize);
			Graphics g = img.getGraphics();
			g.setColor(colorBack);
			g.fillRect(0,0,width*pixelSize,height*pixelSize);
			g.setColor(colorFront);
					  
			for (int x1 = 0; x1 < 8; x1++){
				if ((p & (1 << (7 - x1))) != 0){
					g.fillRect(x1 * pixelSize, 0, pixelSize, pixelSize);
                                }
                        }
			patterns.put(p, img);
		}

		return img;
	}

}



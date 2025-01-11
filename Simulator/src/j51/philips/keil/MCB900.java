/**
 * $Id: MCB900.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.philips.keil;

import j51.intel.*;
import j51.swing.*;

import java.awt.*;
import javax.swing.*;
import java.net.*;

class MCB900PCB extends JLabel
{
	private int led = 0;
	private final Color on = Color.green;
	private final Color off = Color.gray;

	MCB900PCB()
	{
		URL url = ClassLoader.getSystemResource("j51/keil/images/mcb900.jpg");
		ImageIcon icon = new ImageIcon(url);
		setIcon(icon);
	}

        @Override
	public void paint(Graphics g)
	{

		super.paint(g);
			
		for (int i = 0 ; i < 8 ; i++)
		{
			Color c = off;
			if ((led & (1 << i)) != 0)
				c = on;
			g.setColor(c);
			g.fill3DRect(185+(7-i)*13,5,8,12,false);
		}
	}

	void setLed(int led)
	{
		if (this.led != led)
		{
			this.led = led;
			repaint(185,5,8*13,12);
		}
	}

	int getLed()
	{
		return this.led;
	}
}

class MCB900Port extends JPanel implements MCS51Peripheral, SfrWriteListener,UpdatableComponent
{
	MCB900PCB pcb = new MCB900PCB();
	int newLed;
	
	MCB900Port()
	{
		super(new GridBagLayout());

		JFactory.setTitle(this,"MCB900");

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.NONE; 
		g.insets = new Insets(1,1,1,1);

		add(pcb,g);
	}
	
        @Override
	public void registerCpu(MCS51 cpu)
	{
		cpu.addSfrWriteListener(MCS51Constants.P2,this);
		cpu.addUpdatableComponent(this);

	}


        @Override
	public void sfrWrite(int reg,int value)
	{
		newLed = value;
		
	}

        @Override
	public boolean update()
	{
		if (newLed == pcb.getLed())
			return false;
		pcb.setLed(newLed);
		return true;
	}
	
}

/**
 *
 *
 * Keil MCB900 Evaluation board.
 *
 * The following application are tested :
 * 
 *  - Measure
 *  - Blinky
 *
 * @author Mario Viara
 * @version 1.00
 *
 */
public class MCB900 extends j51.philips.LPC900
{
	public MCB900() throws Exception
	{
		super("MCB900");
		addPeripheral(new MCB900Port());

		JPort port = (JPort)getPeripheralByClass("j51.intel.JPort");
		for (int i = 0 ; i < 8 ; i++){
			port.setPortName(2, i, "LED" + i);
                }
	}
	
}



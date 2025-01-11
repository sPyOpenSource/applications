/**
 * $Id: LPC900Misc.java 74 2010-07-07 05:50:03Z mviara $
 */
package j51.philips;

import j51.intel.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import j51.util.Logger;
import j51.swing.*;

/**
 * LPC900 misc register
 *
 * @author Mario Viara
 * @version 1.00
 **/
public class LPC900Misc extends JPanel implements MCS51Peripheral
{
	private static Logger log = Logger.getLogger(LPC900Misc.class);
	LPC900 cpu;
	JBitField ucfg1 = new JBitField("UCFG1",false);
	JBitField bootv = new JBitField("BOOTV",false);
	JBitField boots = new JBitField("BOOTS",false);

	LPC900Misc()
	{
		super(new GridBagLayout());

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = g.CENTER; g.fill = g.NONE; 
		g.insets = new Insets(1,1,1,1);

		add(ucfg1,g);
		g.gridx++;
		add(bootv,g);
		g.gridx++;
		add(boots,g);
	}

	public void registerCpu(MCS51 _cpu)
	{
		this.cpu = (LPC900)_cpu;

		cpu.addUpdatableComponent(ucfg1);
		cpu.addUpdatableComponent(boots);
		cpu.addUpdatableComponent(bootv);
		ucfg1.setValue(cpu.miscRead(cpu.UCFG1));
		bootv.setValue(cpu.miscRead(cpu.BOOTV));
		boots.setValue(cpu.miscRead(cpu.BOOTSTAT));

		ucfg1.setEditable(true);
		boots.setEditable(true);
		bootv.setEditable(true);

		ucfg1.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.miscWrite(cpu.UCFG1,ucfg1.getValue());
				}
				catch (Exception ex)
				{
				}
			}

		});

		bootv.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.miscWrite(cpu.BOOTV,bootv.getValue());
				}
				catch (Exception ex)
				{
				}
			}

		});

		boots.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.miscWrite(cpu.BOOTSTAT,boots.getValue());
				}
				catch (Exception ex)
				{
				}
			}

		});

	}

}


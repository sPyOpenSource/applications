/**
 * $Id: JPort.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.swing;

import j51.intel.*;
import java.awt.*;
import javax.swing.*;

class BytePort extends JBitField implements SfrWriteListener,MCS51Peripheral,ResetListener
{
	private MCS51 cpu;
	private int sfr = -1;
	private int disableMask;

	public BytePort(String name)
	{
		super(name,false);
		
		addActionListener((java.awt.event.ActionEvent e) -> {
                    cpu.addRunQueue(() -> {
                        cpu.sfr(sfr,getValue());
                    });
                });
	}

        @Override
	public void reset(MCS51 cpu)
	{
		setValue(0);
	}
	
	public void setSfr(int sfr)
	{
		this.sfr = sfr;
	}

	public int getSfr()
	{
		return sfr;
	}

	public void setDisableMask(int mask)
	{
		for (int i = 0  ; i < 8 ; i ++)
		{
			if ((mask & (1 << i)) != 0)
				setDisabled(i,true);
		}
	}

        @Override
	public void sfrWrite(int reg,int value)
	{
		setValue(value);
	}

        @Override
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		
		if (sfr != -1)
		{
			cpu.addSfrWriteListener(sfr,this);
			cpu.addUpdatableComponent(this);
		}

		cpu.addResetListener(this);
	}

}

class MCS51Port extends JPanel implements MCS51Peripheral,SfrWriteListener
{
	private final BytePort	m1;
	private final BytePort	m2;
	private final BytePort	p;
	private MCS51		cpu;
	
	public MCS51Port(int port)
	{
		super(new GridBagLayout());

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.NONE; 

		m1 = new BytePort("P"+port+"M1");
		m2 = new BytePort("P"+port+"M2");
		p  = new BytePort("P"+port);
		
		add(m1,g);
		g.gridx++;
		add(m2,g);
		g.gridx++;
		add(p,g);
		
	}

        @Override
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		m1.registerCpu(cpu);
		m2.registerCpu(cpu);
		p.registerCpu(cpu);

		if (m1.getSfr() != -1)
			cpu.addSfrWriteListener(m1.getSfr(),this);
		else
			m1.setDisableMask(0xff);
		if (m2.getSfr() != -1)
			cpu.addSfrWriteListener(m2.getSfr(),this);
		else
			m2.setDisableMask(0xff);
	}

        @Override
	public void sfrWrite(int r,int v)
	{
		int m1Mask = cpu.sfr(m1.getSfr());
		int m2Mask = cpu.sfr(m2.getSfr());

		for (int i = 0 ; i < 8 ; i ++)
		{
			int mask = 1 << i;
			boolean m1b = (m1Mask & mask) != 0;
			boolean m2b = (m2Mask & mask) != 0;
			if (m1b == false && m2b == true)
			    p.setDisabled(i,true);
			else
				p.setDisabled(i,false);
		}
		
	}

	public void setPortName(int bit,String name)
	{
		p.setBitName(bit,name);
	}
	public void setSfrM1(int sfr)
	{
		m1.setSfr(sfr);
	}

	public void setSfrM2(int sfr)
	{
		m2.setSfr(sfr);
	}

	public void setSfrP(int sfr)
	{
		p.setSfr(sfr);
	}

	public void setDisableMask(int mask)
	{
		m1.setDisableMask(mask);
		m2.setDisableMask(mask);
		p.setDisableMask(mask);
	}
	
}

/**
 *
 * 8051 standard I/O port implementation.
 * 
 * @author Mario Viara
 * @version 1.00
 */
public class JPort extends JPanel implements MCS51Peripheral,MCS51Constants
{
	private final int sfrPort[]	= {P0,P1,P2,P3,0,0,0,0,0,0};
	private final int sfrM1[]	= {P0M1,P1M1,P2M1,P3M1,-1,-1,-1,-1,-1,-1};
	private final int sfrM2[]	= {P0M2,P1M2,P2M2,P3M2,-1,-1,-1,-1,-1,-1};
	private final int defaultM1[]   = {0,0,0,0,0,0,0,0,0,0};
	private final int defaultM2[]   = {0,0,0,0,0,0,0,0,0,0};

	MCS51Port ports[];
	
	public JPort(int numPort)
	{
		super(new GridBagLayout());

		ports = new MCS51Port[numPort];

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.NONE; 

		
		for (int i = 0  ; i < numPort ; i ++)
		{
			ports[i] = new MCS51Port(i);
			ports[i].setSfrM1(sfrM1[i]);
			ports[i].setSfrM2(sfrM2[i]);
			ports[i].setSfrP(sfrPort[i]);
			add(ports[i],g);
			g.gridy++;
		}
	}

	public void setPortName(int port,int bit,String name)
	{
		ports[port].setPortName(bit,name);
	}

	public void setDisableMask(int port,int mask)
	{
		ports[port].setDisableMask(mask);
	}
	
        @Override
	public void registerCpu(MCS51 cpu)
	{
            for (MCS51Port port : ports) {
                port.registerCpu(cpu);
            }	
	}

	public void setSfrM1(int i,int sfr)
	{
		ports[i].setSfrM1(sfr);
	}

	public void setSfrM2(int i,int sfr)
	{
		ports[i].setSfrM2(sfr);
	}

	public void setSfrP(int i,int sfr)
	{
		ports[i].setSfrP(sfr);
	}
	
}

/**
 * $Id: JInfo.java 72 2010-07-02 06:56:21Z mviara $
 *
 * 8051 Simulator Information panel.
 *
 * @author Mario Viara
 * @version 1.01
 *
 */
package j51;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.text.*;
import java.beans.*;
   
import j51.intel.*;
import j51.swing.*;

class Block extends JComponent
{
	private Color color;
	private final Dimension size;
	private int perc;
	
	Block(Color color,int w,int h)
	{
		this.color = color;
		size = new Dimension(w,h);
		setPreferredSize(size);
	}

        @Override
	public void paintComponent(Graphics g)
	{
		int h = (size.height * perc ) / 100;
		if (h <= 0)
			h = 1;
		if (h > size.height)
			h = size.height;
		g.setColor(color);
		
		g.fillRect(0,size.height-h,size.width,h);
	}

	public void setColor(Color color)
	{
		this.color = color;
		repaint();
	}

	public void setUsage(int perc)
	{
		this.perc = perc;
		repaint();
	}
}

class CpuUsage extends JPanel
{
	JFixedField perc = new JFixedField(4,true);
	Block	  block;
	JLabel	  label;

	CpuUsage(String name,Color c)
	{
		super(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.fill  = GridBagConstraints.BOTH;g.insets = new Insets(2,2,2,2);
		g.anchor = GridBagConstraints.CENTER;
		Dimension dim = perc.getPreferredSize();
		block = new Block(c,dim.width,dim.height*3);
		label = new JLabel(name);
		perc.setEditable(false);
		perc.setHorizontalAlignment(JFixedField.RIGHT);

		add(label,g);g.gridy++;
		add(block,g);g.gridy++;
		add(perc,g);
	}

	public void setUsage(int usage)
	{
		perc.setText(usage+"%");
		block.setUsage(usage);
	}
}

public class JInfo extends J51Panel implements MCS51Performance,ResetListener
{
	JFormattedTextField oscillator = new JFormattedTextField(new DecimalFormat("###,###,##0"));
	JFormattedTextField clock = new JFormattedTextField(new DecimalFormat("###,###,##0"));
	SpinnerNumberModel cycleModel = new SpinnerNumberModel(1,1,16,1);
	JSpinner	cycle = new JSpinner(cycleModel);
	JHexWord	reset;
	JTextField	elapsed;
	
	CpuUsage  min,max,avg,cur;
	private int minCpuUsage,maxCpuUsage,avgCpuUsage;

	public JInfo()
	{
		super("Information");
		GridBagConstraints g = new GridBagConstraints();

		//oscillator	= new JNumField(15,true);
		oscillator.setColumns(15);
		clock.setColumns(15);
		reset		= new JHexWord(true);
		elapsed		= new JTextField(30);
		min		= new CpuUsage("Min",Color.yellow);
		max		= new CpuUsage("Max",Color.red);
		avg		= new CpuUsage("Avg",Color.green);
		cur		= new CpuUsage("Cur",Color.blue);

		min.setUsage(0);
		max.setUsage(100);
		avg.setUsage(50);
		cur.setUsage(25);
		
		JFactory.setBox(oscillator);
		JFactory.setBox(clock);
		JFactory.setBox(cycle);

		JFactory.setBox(reset);

		oscillator.addPropertyChangeListener("value", (PropertyChangeEvent e) -> {
                    Object o = oscillator.getValue();
                    int value = 0;
                    if (o instanceof Number)
                        value = ((Number)o).intValue();
                    //int value = (Integer)oscillator.getValue();
                    cpu.setOscillator(value);
                });
		
		cycle.addChangeListener((ChangeEvent e) -> {
                    cpu.machineCycle((int)(Integer)cycle.getValue());
                });

		oscillator.setToolTipText("Selected oscillator in Hz");
		cycle.setToolTipText("Machina cycle in oscillator clock");
		clock.setToolTipText("Current clock counter");
		clock.setEditable(false);

		oscillator.setHorizontalAlignment(JFixedField.RIGHT);
		clock.setHorizontalAlignment(JFixedField.RIGHT);

		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.fill  = GridBagConstraints.NONE;g.insets = new Insets(2,2,2,2);
		g.gridheight = 3;
		
		add(cur,g);g.gridx++;
		add(min,g);g.gridx++;
		add(max,g);g.gridx++;
		add(avg,g);g.gridx++;
		
		g.gridx = 4; g.gridwidth = 1;g.gridheight = 1;
		g.anchor = GridBagConstraints.WEST;
		add(new JLabel("Oscillator"),g);
		g.gridx++;g.anchor = GridBagConstraints.EAST;
		add(oscillator,g);
		g.gridx++;
		g.anchor = GridBagConstraints.WEST;
		add(new JLabel("Cycle"),g);
		g.gridx++;g.anchor = GridBagConstraints.EAST;
		add(cycle,g);
		g.gridx++;g.gridy++;
		g.gridx = 4;
		
		g.anchor = GridBagConstraints.WEST;
		add(new JLabel("Clock"),g);
		g.gridx++;g.anchor = GridBagConstraints.EAST;
		add(clock,g);
		g.gridx++;
		
		add(new JLabel("Reset at"),g);
		g.gridx++;
		add(reset,g);

		g.gridy++;g.gridx = 4; g.gridwidth = 1;g.gridheight = 1;
		g.anchor=GridBagConstraints.SOUTHWEST;
		add(new JLabel("Elapsed"),g);
		g.gridx++;g.gridwidth = 3;
		add(elapsed,g);




	}

        @Override
	public void reset(MCS51 cpu)
	{
		minCpuUsage = 100;
		maxCpuUsage = 0;
		avgCpuUsage = 0;
		updateUsage(0);
	}

	private void updateUsage(int cpu)
	{
		min.setUsage(minCpuUsage);
		max.setUsage(maxCpuUsage);
		avg.setUsage(avgCpuUsage);
		cur.setUsage(cpu);
		
	}
	
        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		reset(cpu);
		cpu.addPerformanceListener(this);
		cpu.addResetListener(this);
		
	}
	
        @Override
	public void cpuPerformance(int cpu,int elapsed)
	{
		avgCpuUsage = (cpu + avgCpuUsage) / 2;
		if (cpu < minCpuUsage)
			minCpuUsage = cpu;
		if (cpu > maxCpuUsage)
			maxCpuUsage = cpu;
		updateUsage(cpu);
	}

	
        @Override
	public void setEmulation(boolean mode)
	{
			
		oscillator.setEditable(!mode);
		cycle.setEnabled(!mode);
		reset.setEditable(!mode);
	}

	public long updateClock(StringBuffer sb,String name,long v,long d)
	{
		long n = v / d;
		
		if (n > 0)
		{
			sb.append(n).append(" ").append(name).append(" ");
			v -= d * n;
		}

		return v;
		
	}
	
	public void updateClock()
	{
		long elapsed = cpu.clock();
		long tmp;
		
		clock.setValue(elapsed);
		elapsed *= 1000000;
		elapsed /= cpu.getOscillator();
		StringBuffer s = new StringBuffer();
		elapsed = updateClock(s,"day" ,elapsed,1000000L * 60L * 60L * 24L);
		elapsed = updateClock(s,"hour",elapsed,1000000L * 60L * 60L);
		elapsed = updateClock(s,"min", elapsed,1000000L * 60L);
		elapsed = updateClock(s,"sec", elapsed,1000000L);
		elapsed = updateClock(s,"ms",  elapsed,1000L);
		elapsed = updateClock(s,"us",  elapsed,1L);
		
		this.elapsed.setText(s.toString());
			
		
	}

        @Override
	public void update(boolean force)
	{
		oscillator.setValue(cpu.getOscillator());
		updateClock();
		cycle.setValue(cpu.machineCycle());
	}

}

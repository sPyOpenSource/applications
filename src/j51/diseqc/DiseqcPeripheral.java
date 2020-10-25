/**
 * $Id: DiseqcPeripheral.java 63 2010-06-30 06:24:49Z mviara $
 */
package j51.diseqc;

import java.awt.*;
import javax.swing.*;

import j51.intel.*;
import j51.swing.*;

/**
 * Diseqc peripheral.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	Added separate component for Motor.
 */
public class DiseqcPeripheral extends JPanel implements MCS51Peripheral,
	SfrWriteListener,MemoryReadListener,AsyncTimerListener,ResetListener,EmulationListener
{
	private final int MIN = -900;
	private final int MAX =  900;

	private MCS51 cpu;
	private JLed ledw = new JLed("West",Color.green);
	private JLed lede = new JLed("East",Color.green);
	private JLed led  = new JLed("Status",Color.blue);
	private JLed ledp = new JLed("Power",Color.red);
	private JToggleButton gow = new JToggleButton("GO WEST");
	private JToggleButton goe = new JToggleButton("GO EAST");
	//private Motor m = new JMotor(MIN,MAX,0);
	private Motor m = new Satellite();
	private int motorPosition = 0;
	private int pulseWidth,pulseTime;
	boolean motorPower = false;
	private JSlider sDelay = new JSlider(0,100);
	private JSlider sWidth = new JSlider(0,100);
	
	public DiseqcPeripheral()
	{
		super(new GridBagLayout());
		JFactory.setTitle(this,"Diseqc motor");

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.anchor = g.CENTER; g.fill = g.NONE; g.insets=new Insets(2,2,2,2);



		sWidth.setValue(15);
		sWidth.setMajorTickSpacing(10);
		sWidth.setPaintLabels(true);
		sWidth.setPaintTicks(true);
		sWidth.setPaintTrack(true);

		sDelay.setValue(50);
		sDelay.setMajorTickSpacing(10);
		sDelay.setPaintLabels(true);
		sDelay.setPaintTicks(true);
		sDelay.setPaintTrack(true);

		g.gridx = 0;g.gridwidth = 1;
		add(new JLabel("Pulse witdh (ms)"),g);g.gridx++;
		add(sWidth);g.gridx++;
		g.gridwidth = 2;g.gridheight = 2;add(m.getComponent(),g);
		g.gridwidth = 1;g.gridheight =1;g.gridx = 0;g.gridy++;
		add(new JLabel("Pulse period (ms)"),g);g.gridx++;
		add(sDelay,g);

		g.gridy++;g.gridx=0;g.gridwidth = 1;
		add(ledw,g);
		g.gridx += 1;
		add(led,g);
		g.gridx += 1;
		add(ledp,g);
		g.gridx += 1;
		add(lede,g);

		g.gridy++;g.gridx = 0;g.gridwidth = 2;
		add(gow,g);
		g.gridx += 2;
		add(goe,g);


	}

	public void reset(MCS51 _cpu)
	{
		Diseqc cpu = (Diseqc)_cpu;

		// Check eeprom validity to move the motor on the
		// correct position from eeprom.

		// Signature ?
		if (cpu.eeprom(0) != 0x55AA)
			return;

		int pos = (cpu.eeprom(2) - 32768)  ;
		motorPosition = pos;
		if (motorPosition < MIN)
			motorPosition = MIN;
		if (motorPosition > MAX)
			motorPosition = MAX;
		m.setPosition(motorPosition);
	}


	public int readMemory(int reg,int value)
	{
		value |= 3;

		if (goe.isSelected())
			value &= ~1;
		if (gow.isSelected())
			value &= ~2;

		return value;
	}

	public void writeP1(int value)
	{
		lede.set((value & 0x80) != 0);
		ledw.set((value & 0x40) != 0);
	}

	public void sfrWrite(int reg,int value)
	{
		if (reg == MCS51Constants.P0)
		{
			boolean newMotorPower = (value & 8) == 0;

			if (newMotorPower && !motorPower)
			{
				cpu.addAsyncTimerListenerMillis(pulseTime,DiseqcPeripheral.this);
			}

			motorPower = newMotorPower;

			led.set((value & 0x04) == 0);
			ledp.set(motorPower);


		}
		else
		{
			lede.set((value & 0x80) != 0);
			ledw.set((value & 0x40) != 0);
		}

	}


	public void expired(MCS51 c)
	{

		class EndPulse implements AsyncTimerListener
		{
			private int direction;

			EndPulse(int direction)
			{
				this.direction = direction;
			}

			public void expired(MCS51 c1)
			{
				motorPosition += direction;
				m.setPosition(motorPosition);
				cpu.sfrReset(MCS51Constants.P1,0x10);
				if (motorPower)
					cpu.addAsyncTimerListenerMillis(pulseTime,DiseqcPeripheral.this);

			}

		}

		if (!motorPower)
			return;

		int direction = 0;

		if (lede.get())
		{
			if (motorPosition > MIN)
			{
				direction = -1;
			}
		}
		else
		{
			if (motorPosition < MAX)
			{
				direction = 1;
			}
		}

		if (direction != 0)
		{
			motorPosition += direction;
			m.setPosition(motorPosition);
			cpu.sfrSet(MCS51Constants.P1,0x10);
			cpu.addAsyncTimerListenerMillis(pulseWidth,new EndPulse(direction));
		}

	}


	public void setEmulation(boolean mode)
	{
		if (mode)
		{
			pulseTime = sDelay.getValue();
			pulseWidth = sWidth.getValue();
		}
		
		sWidth.setEnabled(!mode);
		sDelay.setEnabled(!mode);
	}
	
	public void registerCpu(MCS51 cpu)
	{
		this.cpu = cpu;
		cpu.addSfrWriteListener(MCS51Constants.P0,this);
		cpu.addSfrWriteListener(MCS51Constants.P1,this);
		cpu.addSfrMemoryReadListener(MCS51Constants.P0,this);
		cpu.addResetListener(this);
		cpu.addEmulationListener(this);
		pulseWidth = 15;
		pulseTime  = 50;
		//pulseWidth *= (int)(cpu.oscillator() / cpu.cycles() / 1000) ;
		//pulseTime  *= (int)(cpu.oscillator() / cpu.cycles() / 1000) ;


	}
}
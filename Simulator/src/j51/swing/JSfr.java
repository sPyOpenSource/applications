
package j51.swing;

import j51.util.Hex;
import jCPU.iCPU;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;

/**
 *
 * @author xuyi
 */
public class JSfr extends J51Panel
{
	JLabel labels[] = new JLabel[128];
	JHexByte fields[] = new JHexByte[128];
	private Timer updateTimer;
	
	public JSfr()
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
	public void setCpu(iCPU cpu)
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
			try {
				fields[i].setValue(cpu.sfr(i + 128));
			} catch (UnsupportedOperationException ex){
				// SFR is an 8051 concept; CPUs that don't implement it
				// (e.g. ARM) should show empty/0 fields instead of crashing.
				fields[i].setValue(0);
			}
                }
	}
        
        @Override
        public void setEmulation(boolean mode)
        {
            super.setEmulation(mode);
            if (mode) {
                // Start a timer to keep SFR values live-updated during emulation
                if (updateTimer == null) {
                    updateTimer = new Timer(50, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            update(false);
                        }
                    });
                    updateTimer.setRepeats(true);
                }
                updateTimer.start();
            } else {
                if (updateTimer != null) {
                    updateTimer.stop();
                }
            }
        }
}


package j51.swing;

import j51.J51Panel;
import j51.intel.MCS51;
import j51.util.Hex;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;

/**
 *
 * @author xuyi
 */
public class JSfr extends J51Panel
{
	JLabel labels[] = new JLabel[128];
	JHexByte fields[] = new JHexByte[128];
	
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

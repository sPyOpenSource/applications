
package j51.swing;

import j51.J51Panel;
import j51.intel.MCS51;
import j51.intel.MCS51Peripheral;
import java.awt.Component;
import java.awt.GridBagConstraints;

/**
 *
 * @author xuyi
 */
public class JPeripheral extends J51Panel
{
	public JPeripheral()
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

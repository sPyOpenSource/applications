
package j51.swing;

import jCPU.MCS51.CPU;
import jCPU.MCS51.MCS51Peripheral;
import jCPU.iCPU;
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
	public void setCpu(iCPU cpu)
	{
		super.setCpu(cpu);
		removeAll();
		if (!(cpu instanceof CPU mcs51)) return;
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; 
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; 
                g.fill = GridBagConstraints.BOTH;
		for (int i = 0 ; i < mcs51.getPeripheralsCount() ; i++)
		{
			MCS51Peripheral p = mcs51.getPeripheralAt(i);
			
			if (p instanceof Component component)
			{
				add(component, g);
				g.gridy++;
			}
		}
	}
}

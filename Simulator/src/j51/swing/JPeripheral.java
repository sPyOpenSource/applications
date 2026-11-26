
package j51.swing;

import jCPU.MCS51.CPU;
import jCPU.MCS51.MCS51Peripheral;
import jCPU.Peripheral;
import jCPU.iCPU;
import jCPU.x86.X86Core;
import jCPU.x86.X86VgaPeripheral;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;

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
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0; 
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; 
                g.fill = GridBagConstraints.BOTH;

		if (cpu instanceof CPU mcs51)
		{
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
		else if (cpu instanceof jCPU.x86.CPU x86)
		{
			X86Core core = x86.getCore();
			Map<Integer, Peripheral> ports = core.getIoPorts();

			// Sort by port address for a stable display order
			List<Map.Entry<Integer, Peripheral>> sorted = new ArrayList<>(ports.entrySet());
			sorted.sort(Map.Entry.comparingByKey());

			int lastPort = -1;
			for (Map.Entry<Integer, Peripheral> e : sorted)
			{
				int port = e.getKey();
				Peripheral p = e.getValue();

				// Group consecutive ports under a single label
				if (lastPort < 0 || port > lastPort + 1)
				{
					String name = p.getClass().getSimpleName();
					if (p instanceof X86VgaPeripheral)
					{
						name = "VGA";
					}
					else if (p instanceof jCPU.x86.X86UartPeripheral)
					{
						name = "UART COM1";
					}
					else if (p instanceof jCPU.x86.X86IdeController)
					{
						name = "IDE";
					}
					else if (p instanceof jCPU.x86.X86UhciController)
					{
						name = "USB UHCI";
					}
					add(new JLabel(String.format("Port 0x%03X: %s", port, name)), g);
					g.gridy++;
				}
				lastPort = port;

				if (p instanceof Component component)
				{
					add(component, g);
					g.gridy++;
				}
				else if (p instanceof X86VgaPeripheral vga)
				{
					JComponent display = vga.getDisplay();
					add(display, g);
					g.gridy++;
				}
			}
		}
	}
}

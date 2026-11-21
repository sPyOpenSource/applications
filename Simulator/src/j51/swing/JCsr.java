
package j51.swing;

import j51.util.Hex;
import jCPU.iCPU;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Control and Status Register (CSR) panel for RISC-V.
 * Shows CSR name and value for each CSR currently defined in the CPU.
 *
 * @author xuyi
 */
public class JCsr extends J51Panel
{
	private JLabel labels[];
	private JHexByte fields[];
	private int numCsrs = 0;
	
	public JCsr()
	{
		super("CSR", false);
		labels = new JLabel[128];
		fields = new JHexByte[128];
		buildLayout();
	}
	
	private void buildLayout()
	{
		removeAll();
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0 ; g.gridy = 0; g.gridwidth = 1; g.gridheight = 1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(1,1,1,1);

		for (int i = 0 ; i < numCsrs ; i++)
		{
			labels[i] = new JLabel("csr " + i + "  ");
			fields[i] = new JHexByte();
			fields[i].setEditable(false);
			fields[i].setToolTipText("CSR " + i);
			add(labels[i], g);
			g.gridx++;
			add(fields[i], g);
			g.gridx++;
			if (g.gridx == 16)
			{
				g.gridy++;
				g.gridx = 0;
			}
		}
		revalidate();
		repaint();
	}
	
        @Override
	public void setCpu(iCPU cpu)
	{
		super.setCpu(cpu);
		numCsrs = cpu.csrSize();
		buildLayout();
		updateLabels();
	}

	private void updateLabels()
	{
		for (int i = 0 ; i < numCsrs ; i++)
		{
			if (labels[i] != null && cpu != null)
			{
				String s = cpu.getCsrName(i);
				labels[i].setToolTipText(s);
				if (s.length() > 8) {
					s = s.substring(0, 8);
				}
				labels[i].setText(s);
			}
		}
	}

        @Override
	public void update(boolean force)
	{
		if (cpu == null) return;
		int newSize = cpu.csrSize();
		if (newSize != numCsrs) {
			numCsrs = newSize;
			buildLayout();
			updateLabels();
		}
		for (int i = 0 ; i < numCsrs ; i++){
			try {
				fields[i].setValue(cpu.csr(i));
			} catch (Exception ex){
				fields[i].setValue(0);
			}
                }
	}
}

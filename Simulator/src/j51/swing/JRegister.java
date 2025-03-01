
package j51.swing;

import j51.J51Panel;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 *
 * @author xuyi
 */
public class JRegister extends J51Panel
{
    
	private final JLabel la,lb,lsp,lpc,ldpl,ldph,ldptr,lpsw;
	private final JLabel lr[] = new JLabel[8];
	private JHexField a,b,pc,dpl,dph,dptr,sp,psw;
	private final JHexField r[] = new JHexField[8];
	private ActionListener listener = null;
	
	public JRegister()
	{
		super("Register");
		// Create the jlabel
		la	= new JLabel("A");
		lb	= new JLabel("B");
		lsp	= new JLabel("SP");
		lpc	= new JLabel("PC");
		ldpl	= new JLabel("DPL");
		ldph	= new JLabel("DPH");
		ldptr	= new JLabel("DPTR ");
		lpsw	= new JLabel("PSW");
		
		for (int i = 0 ; i < 8 ; i++){
			lr[i] = new JLabel("R" + i);
		}

		
		// Create the text field
		a		= new JHexByte();
		b		= new JHexByte();
		sp		= new JHexByte();
		pc		= new JHexWord();
		dpl		= new JHexByte();
		dph		= new JHexByte();
		dptr		= new JHexWord();
		psw		= new JHexByte();

		
		for (int i = 0 ; i < 8 ; i++)
		{
			r[i] = new JHexByte();
		}

		GridBagConstraints g = new GridBagConstraints();
		//g.insets = new Insets(1,1,1,1);
		
		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.fill  = GridBagConstraints.NONE;

		addRegister(la,a,g);
		addRegister(lb,b,g);

		for (int i = 0 ; i < 8 ; i++)
		{
			addRegister(lr[i], r[i], g);
			r[i].addActionListener(new ActionListener()
			{
                                @Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{
						cpu.pc(pc.getValue());
						if (listener != null){
							listener.actionPerformed(new ActionEvent(this, 0, "PC"));
                                                }
					} catch (Exception ex) {
					}
				}
			});
		}				
				

		addRegister(lsp, sp, g);
		addRegister(lpsw, psw, g);
		addRegister(lpc, pc, g);
		addRegister(ldpl, dpl, g);
		addRegister(ldph, dph, g);
		addRegister(ldptr, dptr, g);


		/**
		 * Add action listener
		 */
		pc.addActionListener(new ActionListener()
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.pc(pc.getValue());
					if (listener != null){
						listener.actionPerformed(new ActionEvent(this, 0, "PC"));
                                        }
				} catch (Exception ex) {
				}
			}
		});

		sp.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.sp(sp.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });

		a.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.acc(a.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });
		
		b.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.b(b.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });
		
		psw.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.psw(psw.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });

		dpl.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.dpl(dpl.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });

		dph.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.dph(dph.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });


		dptr.addActionListener((ActionEvent e) -> {
                    try
                    {
                        cpu.dptr(dptr.getValue());
                        fireChangeSfr();
                    } catch (Exception ex) {
                    }
                });

	}

	private void fireChangeSfr()
	{
		if (listener != null){
			listener.actionPerformed(new ActionEvent(this, 0, "SFR"));
                }
	}
	
	public void setChangeListener(ActionListener l)
	{
		listener = l;
	}
	
	private void addRegister(JLabel label, JHexField field, GridBagConstraints g)
	{
		g.fill = GridBagConstraints.HORIZONTAL;
		label.setBorder(BorderFactory.createEtchedBorder());
		field.setBorder(BorderFactory.createEtchedBorder());
		g.anchor = GridBagConstraints.CENTER;
                g.gridx = 0;
		add(label, g);		
                g.anchor = GridBagConstraints.EAST;
                g.gridx++;
		add(field, g);
		g.gridy++;
	}
	
        @Override
	public void update(boolean force)
	{
		a.setValue(cpu.acc());
		b.setValue(cpu.b());
		sp.setValue(cpu.sp());
		pc.setValue(cpu.pc());
		dpl.setValue(cpu.dpl());
		dph.setValue(cpu.dph());
		dptr.setValue(cpu.dptr());
		psw.setValue(cpu.psw());
		
		for (int i = 0 ; i < 8 ; i++)
		{
			r[i].setValue(cpu.r(i));
		}
		
	}

        @Override
	public void setEmulation(boolean mode)
	{
		a.setEditable(!mode);
		b.setEditable(!mode);
		sp.setEditable(!mode);
		pc.setEditable(!mode);
		dpl.setEditable(!mode);
		dph.setEditable(!mode);
		dptr.setEditable(!mode);
		psw.setEditable(!mode);
		
		for (int i = 0 ; i < 8 ; i++){
			r[i].setEditable(!mode);
		}
	}
	
}


package j51.swing;

import j51.J51Panel;
import j51.util.Hex;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

/**
 *
 * @author xuyi
 */
public abstract class JData extends J51Panel
{
    
	private int top, bottom, base, row;
	protected JHexByte bytes[] = new JHexByte[16 * 16];
	private final JLabel address[] = new JLabel[16];
	private JHexField add;
	private boolean scrollable = false;
	private JHexWord jtop;
	
	JData(String title, int _bottom, int _top)
	{
		super(title, false);
		
		this.top = _top;
		this.bottom =  _bottom;
		
		for (int i = 0 ; i < 256 ; i++){
			bytes[i] = new JHexByte();
			bytes[i].setToolTipText(Hex.bin2byte(i));
		}
		
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.fill  = GridBagConstraints.NONE;g.insets = new Insets(1,1,1,1);
		g.anchor = GridBagConstraints.WEST;

		row = (top - bottom) / 16;
		if (row > 16)
		{
			scrollable = true;
			row = 16;
			
			add = new JHexWord(true);
			add(new JLabel("Base"),g);
			g.gridx++;g.gridwidth = 3;
			add(add,g);
			g.gridx += 3;
			add(new JLabel("Bottom"),g);
			g.gridx += 3;
			add(new JHexWord(bottom,true),g);
			g.gridx += 3;
			add(new JLabel("Top"),g);
			g.gridx += 3;
			add(jtop = new JHexWord(top - 1,true),g);
					   
			g.gridy++;
			
			add.addActionListener((ActionEvent e) -> {
                            try {
                                int address1 = add.getValue();
                                if (address1 < bottom) {
                                    address1 = bottom;
                                }
                                if (address1 + 256 > top) {
                                    address1 = top - 256;
                                }
                                setAddress(address1);
                            } catch (Exception ex) {
                            }
                        });

		} else {
			add = null;
                }

		g.gridx = 0;
		g.gridwidth = 1;
		for (int i = 0 ; i < 16 ; i++)
		{
			g.gridx++;
			JLabel l = new JLabel(Hex.bin2byte(i));
			JFactory.setBox(l);
			add(l,g);
			address[i] = new JLabel(Hex.bin2word(i*16));
			
			JFactory.setBox(address[i]);

		}
		
		g.gridy++;
		
		for (int i = 0; i < row; i++)
		{
			g.gridx = 0;
			add(address[i],g);
			for (int j = 0; j < 16; j ++)
			{
				g.gridx++;
				add(bytes[i * 16 + j], g);
			}
			g.gridy++;
		}

		setAddress(bottom);
	}

        @Override
	public void setEmulation(boolean mode)
	{
		if (add != null)
			add.setEditable(!mode);
	}

	void setAddress(int base)
	{
		this.base = base;
		for (int i = 0; i < row; i++){
			address[i].setText(Hex.bin2word(base + 16 * i));
		}
		
		update(false);
	}

        @Override
	public void update(boolean force)
	{
		for (int i = 0 ; i < row ; i++){
			for (int j = 0 ; j < 16 ; j++){
				bytes[i * 16 + j].setValue(getByte(base + i * 16 + j));
                        }
                }
	}

	public void setTop(int top)
	{
		this.top = top;
		jtop.setValue(top-1);
	}
	
	abstract public int getByte(int address);
	
}

/**
 * $Id: JAssembly.java 56 2010-06-24 20:06:35Z mviara $
 */

package j51.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import j51.util.Hex;
import j51.intel.*;
import j51.util.FastArray;
import j51.J51Panel;

/**
 *
 * 8051 Simulator Assembler panel.
 *
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	Changed ArrayList() with FastArray().
 */
public class JAssembly extends J51Panel
{
	private final AbstractTableModel tm;
	private final JTable jt;
	private FastArray breakPoint = new FastArray();
	private FastArray pcs = new FastArray();
	private boolean completed = false;
	
	class Renderer  extends DefaultTableCellRenderer
	{
                @Override
		public Component getTableCellRendererComponent(JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			Font font = getFont();

			font = new Font("Monospaced", font.getStyle(), font.getSize());
			setFont(font);

			return this;
		}
	};

	public JAssembly()
	{
		super("Assembler", false);

		tm = new AbstractTableModel()
		{
                        @Override
			public int getRowCount()
			{
				return completed ? pcs.size() : pcs.size() + 1;
			}

                        @Override
			public int getColumnCount()
			{
				return 3;
			}

                        @Override
			public Object getValueAt(int r, int c)
			{
				int pc;
				
				if (r >= pcs.size()){
					decode(-1,r);
				}
				
				if (r >= pcs.size())
					return null;
				pc = pc(r);
				switch (c){
					case	0:
						return breakPoint.get(r);
						
					case	1:
						return Hex.bin2word(pc);
						
					case	2:
						return cpu.getDecodeAt(pc).substring(5);
					default:
						return null;
				}
			}

                        @Override
			public String getColumnName(int c)
			{
				switch (c){
					case	0:
						return "";
					case	1:
						return "Addr";
					case	2:
						return "Code";
					default:
						return null;
				}
			}

                        @Override
			public Class getColumnClass(int c)
			{
				return getValueAt(0, c).getClass();
			}

                        @Override
			public boolean isCellEditable(int rowIndex, int columnIndex)
			{
				return columnIndex == 0;
			}

                        @Override
			public void setValueAt(Object v,int r,int c)
			{
				Boolean b = (Boolean)v;
				breakPoint.set(r,b);
				cpu.setBreakPoint(pc(r), b);
			}
		};


		jt = new JTable(tm);
		jt.setDefaultRenderer(String.class,new Renderer());
		JScrollPane sc = new JScrollPane(jt);
		sc.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS );

		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;
		g.fill  = GridBagConstraints.BOTH;
                g.insets = new Insets(1, 1, 1, 1);
		g.anchor = GridBagConstraints.CENTER;

		add(sc,g);
	}

	private int pc(int r)
	{
		return ((Integer)(pcs.get(r)));
	}
	
	private void decode(int maxPc, int maxRow)
	{
		int pc;
		int count = 0;
		if (pcs.size() == 0){
			pc = 0;
                } else {
			pc = pc(pcs.size() - 1);
			pc += cpu.getLengthAt(pc);
		}
	
		int from = pcs.size();
		while (true){
			if (pc >= cpu.getCodeSize()){
				completed = true;
				break;
			}
			if (maxRow >= 0 && pcs.size() > maxRow)
				break;
			if (maxPc >= 0 && pc > maxPc)
				break;
			count++;
			pcs.add(pc);
			breakPoint.add(false);
			pc += cpu.getLengthAt(pc);
			tm.fireTableRowsInserted(pcs.size() - 1, pcs.size() - 1);
		}

		//System.out.println("Disassembly " + from + " count " + count + " completed " + completed + " maxpc " + maxPc + " maxrow " + maxRow);
	}
	
	private void createTable()
	{
		pcs.clear();
		breakPoint.clear();
		completed = false;

		for(int i = 0; i < tm.getColumnCount(); i++) 
		{ 
			TableColumn c = jt.getColumnModel().getColumn(i);
			Component comp;
			switch (i)
			{
				default:
				case	2:
					comp = new JTextField(50);
					break;
				case	1:
					comp = new JTextField(4);
					break;
				case	0:
					comp = new JCheckBox();
					break;
			}
			Dimension dim = comp.getPreferredSize();
			c.setPreferredWidth(dim.width);
			//c.setMinWidth(dim.width);
			//c.setMaxWidth(dim.width);
			c.setResizable(false);
		}
		tm.fireTableDataChanged();
	}

        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		createTable();
	}


        @Override
	public void update(boolean force)
	{
		if (force){
			createTable();
                }
		int row = 0;
		decode(cpu.pc(), -1);
		
		for (int i = 0 ; i < pcs.size() ; i++){
			//System.out.println("Row "+i+" = "+Hex.bin2word(pc(i))+" Search "+Hex.bin2word(cpu.pc()));

			if (pc(i) >= cpu.pc()){
				row = i;
				break;
			}
		}

		Rectangle rect = jt.getCellRect(row, 0, true);
		jt.scrollRectToVisible(rect);
		jt.clearSelection();
		jt.setRowSelectionInterval(row, row);
	}

        @Override
	public void setEmulation(boolean mode)
	{
		jt.setEnabled(!mode);
	}
}

/**
 * $Id: J51.java 70 2010-07-01 09:57:00Z mviara $
 */
package j51;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.util.*;
import java.io.*;
import java.net.*;

import j51.util.*;
import j51.intel.*;
import j51.swing.*;
import java.util.logging.Level;

class SortedLong extends java.util.TreeMap
{
	void put(long key,String value)
	{
		Long l = key;
		Object o;
		java.util.Vector vector;
		
		vector = (Vector)get(l);
		
		if (vector == null)
		{
			vector = new java.util.Vector();
			put(l,vector);
		}
		
		vector.add(value);
	}

	JTree createTree()
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		java.util.Vector keys  = new java.util.Vector();

		Set set = keySet();
		Iterator iter = set.iterator();

		while (iter.hasNext())
			keys.add(iter.next());
		
		for (int i = 0; i < keys.size() ; i++)
		{
			Long l = (Long)keys.elementAt(keys.size() - 1 -i);
			String s = l+"";
			while (s.length() < 16)
				s = " "+s;
			
			java.util.Vector v = (java.util.Vector)get(l);
			if (v.size() > 1)
			{
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(s);
				root.add(node);
			
				for (int j = 0 ; j < v.size() ; j++)
				{
					DefaultMutableTreeNode n1 = new DefaultMutableTreeNode(v.elementAt(j).toString());
					node.add(n1);
				}
			} else {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(s+" "+v.elementAt(0).toString());
				root.add(node);
			}
		}
			
		return new JTree(root);
	}
	
}


class JRegister extends J51Panel
{
	private final JLabel la,lb,lsp,lpc,ldpl,ldph,ldptr,lpsw;
	private final JLabel lr[] = new JLabel[8];
	private JHexField a,b,pc,dpl,dph,dptr,sp,psw;
	private final JHexField r[] = new JHexField[8];
	private ActionListener listener = null;
	
	JRegister()
	{
		super("Register");
		// Create the jlabel
		la		= new JLabel("A");
		lb		= new JLabel("B");
		lsp		= new JLabel("SP");
		lpc		= new JLabel("PC");
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
			addRegister(lr[i],r[i],g);
			r[i].addActionListener(new ActionListener()
			{
                                @Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{
						cpu.pc(pc.getValue());
						if (listener != null)
							listener.actionPerformed(new ActionEvent(this,0,"PC"));
					}
					catch (Exception ex)
					{
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
		listener =l;
	}

	
	private void addRegister(JLabel label,JHexField field,GridBagConstraints g)
	{
		g.fill = GridBagConstraints.HORIZONTAL;
		label.setBorder(BorderFactory.createEtchedBorder());
		field.setBorder(BorderFactory.createEtchedBorder());
		g.anchor = GridBagConstraints.CENTER;g.gridx = 0;
		add(label,g);		g.anchor = GridBagConstraints.EAST;g.gridx++;
		add(field,g);
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
		
		for (int i = 0 ; i < 8 ; i++)
		{
			r[i].setEditable(!mode);
		}

	}
	
}

abstract class JData extends J51Panel
{
	private int top,bottom,base,row;
	protected JHexByte	bytes[] = new JHexByte[16*16];
	private final JLabel	address[] = new JLabel[16];
	private JHexField	add;
	private boolean scrollable = false;
	private JHexWord jtop;
	
	
	JData(String title,int _bottom,int _top)
	{
		super(title,false);
		
		this.top = _top;
		this.bottom =  _bottom;
		
		for (int i = 0 ; i < 256 ; i++)
		{
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
		
		for (int i = 0 ; i < row ; i++)
		{
			g.gridx = 0;
			add(address[i],g);
			for (int j = 0 ; j < 16 ; j ++)
			{
				g.gridx++;
				add(bytes[i*16+j],g);
			}
			g.gridy++;
		}

		setAddress(bottom);
	}

	public void setEmulation(boolean mode)
	{
		if (add != null)
			add.setEditable(!mode);
	}

	void setAddress(int base)
	{
		this.base = base;
		for (int i = 0 ; i < row ; i++)
		{
			address[i].setText(Hex.bin2word(base+16*i));
		}
		
		update(false);
	}

	public void update(boolean force)
	{
		for (int i = 0 ; i < row ; i++)
			for (int j = 0 ; j < 16 ; j++)
				bytes[i*16+j].setValue(getByte(base+i*16+j));
	}

	public void setTop(int top)
	{
		this.top = top;
		jtop.setValue(top-1);
	}
	
	abstract public int getByte(int address);
	
}

class JCode extends JData
{
	JCode()
	{
		super("Code", 0, 0x10000);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.code(address);
	}

        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		setTop(cpu.getCodeSize());
	}
	
}

class JXdata extends JData
{
	JXdata()
	{
		super("Xdata",0,0x10000);
	}

        @Override
	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.xdata(address);
	}

        @Override
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		setTop(cpu.getXdataSize());
	}

}

class JIdata extends JData
{
	JIdata()
	{
		super("Idata",0,256);
	}

	public int getByte(int address)
	{
		if (cpu == null)
			return 0;

		return cpu.idata(address);
	}

}

class JSfr extends J51Panel
{
	JLabel labels[] = new JLabel[128];
	JHexByte fields[] = new JHexByte[128];
	
	JSfr()
	{
		super("JSFR",false);
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0 ; g.gridy = 0;g.gridwidth = 1;g.gridheight=1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(1,1,1,1);

		for (int i = 0 ; i < 128 ; i++)
		{
			labels[i] = new JLabel(Hex.bin2byte(i+128)+"  ");
			fields[i] = new JHexByte();
			fields[i].setEditable(false);
			fields[i].setToolTipText("Address " + Hex.bin2byte(i + 128));
			add(labels[i],g);g.gridx++;
			add(fields[i],g);g.gridx++;
			if (g.gridx == 16)
			{
				g.gridy++;
				g.gridx = 0;
			}
		}
		
	}
	
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		for (int i = 0 ; i < 128 ; i++)
		{
			String s= cpu.getSfrName(i+128);
			labels[i].setToolTipText(s);

			if (s.length() > 4)
				s= s.substring(0,4);
			labels[i].setText(s);
		}
	}

	public void update(boolean force)
	{
		for (int i = 0 ; i < 128 ; i++)
			fields[i].setValue(cpu.sfr(i+128));
	}
	
}

class JPeripheral extends J51Panel
{
	JPeripheral()
	{
		super("Peripheral");
	}

	
	public void setCpu(MCS51 cpu)
	{
		super.setCpu(cpu);
		removeAll();
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0 ; g.gridy = 0;g.gridwidth = 1;g.gridheight=1;
		g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.BOTH;
		for (int i = 0 ; i < cpu.getPeripheralsCount() ; i++)
		{
			MCS51Peripheral p = cpu.getPeripheralAt(i);
			
			if (p instanceof Component)
			{
				add((Component)p,g);
				g.gridy++;
			}
		}
	}
}

class Rect extends JComponent
{
	private Color color;
	private final Dimension size;


	Rect(Color color,int w,int h)
	{
		this.color = color;
		size = new Dimension(w,h);
		setPreferredSize(size);
	}

	public void paintComponent(Graphics g)
	{
		g.setColor(color);
		g.fillRect(0,0,size.width,size.height);
	}

	public void setColor(Color color)
	{
		this.color = color;
		repaint();
	}

}



class Worker extends JDialog implements Runnable
{
	private Object result = null;
	private final Rect rectangle[];
	private javax.swing.Timer timer;
	private int paintIndex = 0;
	private Color paintColor = Color.blue;
	private final JLabel progress;
	
	Worker(JFrame parent,String msg1,String msg2)
	{
		super(parent,true);

		setTitle(msg1);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;
		g.fill  = GridBagConstraints.NONE;g.insets = new Insets(5,5,5,5);
		g.anchor = GridBagConstraints.CENTER;

		g.gridwidth = 2;
		p.add(new JLabel(msg1),g);
		g.gridwidth = 1; g.gridy++;
		//p.add(new JLabel(Utils.getIcon(Utils.ICON_PROCESS24)),g);
		g.gridx++;
		progress = new JLabel(msg2);
		p.add(progress,g);

		//setIconImage(Utils.getIcon(Utils.ICON_PROCESS).getImage());
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE );

		JPanel p1 = new JPanel(new GridBagLayout());
		GridBagConstraints g1 = new GridBagConstraints();
		g1.gridx = 0;g1.gridy = 0;g1.gridwidth = 1;g1.gridheight = 1;
		g1.fill  = GridBagConstraints.NONE;g1.insets = new Insets(1,1,1,1);
		g1.anchor = GridBagConstraints.CENTER;

		rectangle = new Rect[10];
		for (int i = 0 ; i < rectangle.length ; i++)
		{
			rectangle[i] = new Rect(Color.gray,20,15);
			p1.add(rectangle[i],g1);
			g1.gridx++;
		}
		g.gridx = 0; g.gridy++;g.gridwidth = 2;
		p.add(p1,g);
		setContentPane(p);
		pack();
		setLocationCenter(this);
	}

	public void setProgress(String msg)
	{
		progress.setText(msg);
	}
	
        @Override
	public void run()
	{
		try
		{
			process();
		} catch (Exception ex) {
		}

		timer.stop();

		setVisible(false);
		SwingUtilities.invokeLater(() -> {
                    swingProcess();
                });
	}

	public void setResult(Object r)
	{
		result = r;
	}

	public Object getResult()
	{
		return result;
	}

	public void process()
	{
	}

	public void swingProcess()
	{
	}

	void paintRectangle()
	{
		rectangle[paintIndex].setColor(paintColor);

		if (++paintIndex >= rectangle.length)
		{
			paintColor = paintColor ==
				     Color.blue ? Color.gray :
				     Color.blue;
			paintIndex = 0;
		}
	}

	void start()
	{
		Thread t = new Thread(this);
		t.start();
		timer = new javax.swing.Timer(100, (ActionEvent e) -> {
                    paintRectangle();
                });
		timer.start();
		setVisible(true);
	}

	static public void setLocationCenter(Component target)
	{
		Container parent = target.getParent();

		if (parent.isShowing() == false){
			parent = null;
                }

		if (parent == null){
			Dimension dim = target.getToolkit().getScreenSize();
			Rectangle abounds = target.getBounds();
			target.setLocation((dim.width - abounds.width) / 2,
					   (dim.height - abounds.height) / 2);
		} else {
			int x;
			int y;

			Point topLeft = parent.getLocationOnScreen();
			Dimension parentSize = parent.getSize();

			Dimension mySize = target.getSize();

			if (parentSize.width > mySize.width) 
				x = ((parentSize.width - mySize.width)/2) + topLeft.x;
			else 
				x = topLeft.x;

			if (parentSize.height > mySize.height) 
				y = ((parentSize.height - mySize.height)/2) + topLeft.y;
			else 
				y = topLeft.y;

			target.setLocation (x, y);

		}


	}

}

/**
 *
 * J51 main Frame.
 * 
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01	Added support for menu icon.
 * 
 */
public class J51 extends JFrame implements MCS51Performance,ActionListener
{
	private static final Logger log = Logger.getLogger(J51.class);
	
	static  private J51	instance = null;
	private final J51Panel	peripheral;
	private final JRegister	register;
	private final JAssembly	assembly;
	private final JSfr		sfr;
	private final JIdata		idata;
	private final JXdata		xdata;
	private final JCode		code;
	private final JInfo info;
	private final JFixedField messages;
	private JToolBar toolBar = new JToolBar();
	private MCS51 cpu;
	private JFileChooser   fc = null;
	private JButton		   buttonStop;
	private AbstractAction actionDebugTrace;
	private AbstractAction actionDebugStep;
	private AbstractAction actionDebugGo;
	private AbstractAction actionDebugStop;
	private AbstractAction actionDebugReset;
	private AbstractAction actionDebugErase;
	private AbstractAction actionToolsProfile;
	private AbstractAction actionToolsStatistics;
	private AbstractAction actionToolsInterrupt;
	private JMenu menuCpu;
	
	private AbstractAction actionFileLoad;
	private AbstractAction actionFileExit;
	private int minCpuUsage,maxCpuUsage,avgCpuUsage;
	private int cpuTime;
	private Thread thread;
	private JRadioButtonMenuItem first = null;
	private final java.util.Vector panels = new java.util.Vector();


	J51(){
		setTitle("J51 1.05 $Revision: 70 $ - Created by mario@viara.eu");
		pack();
		setVisible(true);
		info	 = new JInfo();
		register = new JRegister();
		assembly = new JAssembly();
		sfr	 = new JSfr();
		idata	 = new JIdata();
		xdata	 = new JXdata();
		code	 = new JCode();
		peripheral= new JPeripheral();
		messages = new JFixedField(64);
		JFactory.setTitle(messages,"Messages");
		JPanel p = new JPanel(new GridBagLayout());
		createMenuBar();
		register.setChangeListener(this);
		
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(1,1,1,1);
		g.gridx = 0;g.gridy = 0;g.gridwidth = 1;g.gridheight = 1;

		g.fill = GridBagConstraints.NONE;
		g.anchor = GridBagConstraints.WEST;
		g.gridwidth = 2;
		p.add(toolBar,g);
		
		g.gridy++;g.fill = GridBagConstraints.BOTH;g.anchor = GridBagConstraints.CENTER;
		
		g.gridwidth = 2;
		p.add(info, g);
		g.gridwidth = 1;
                                           g.gridy++;
		p.add(register,g);
		g.gridx++;
		
		JTabbedPane tp = new JTabbedPane();
		tp.add("Assembler",assembly);
		tp.add("SFR",sfr);
		tp.add("IDATA",idata);
		tp.add("XDATA",xdata);
		tp.add("CODE",code);
		
		p.add(tp,g)
				;
		g.gridx = 0;g.gridy++;g.gridwidth = 2;
		p.add(messages,g);

		g.gridx = 2;g.gridy = 0;
		g.gridheight = 4;g.gridwidth = 1;
		p.add(peripheral,g);
		
		setContentPane(p);

		panels.add(info);
		panels.add(assembly);
		panels.add(sfr);
		panels.add(idata);
		panels.add(xdata);
		panels.add(code);
		panels.add(register);
		panels.add(peripheral);

		setResizable(false);

		addWindowListener(new WindowAdapter()
		{
                        @Override
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		first.doClick();

		instance = this;
	}

	static public J51 getInstance()
	{
		return instance;
	}
	

        @Override
	public void actionPerformed(ActionEvent e)
	{
		String source = e.getActionCommand();
		
		
		if (source.equals("PC"))
		{
			assembly.update(false);
		}

		if (source.equals("SFR"))
		{
			sfr.update(false);
		}
		
	}
	
	public void setCpu(String _name)
	{
		final String name = _name;
		
		Worker w = new Worker(this,"Setup simulator","Loading")
		{
                        @Override
			public void process()
			{
				try
				{
					Class c = Class.forName(name);
					setProgress("Loading class");
					MCS51 newCpu = (MCS51)c.newInstance();
					info.reset.setValue(0);
					J51.this.cpu = newCpu;
					setProgress("Reset cpu");
					reset();

				for (int i = 0 ; i < panels.size() ; i++)
				{
					J51Panel p = (J51Panel)panels.elementAt(i);
					setProgress("Initialize  " + p.getTitle());
					p.setCpu(cpu);
				}

				cpu.addPerformanceListener(J51.this);

				if (J51.this.isVisible())
				{
					J51.this.invalidate();
					J51.this.pack();
				}

				for (int i = 0 ; i < panels.size() ; i++)
				{
					J51Panel p = (J51Panel)panels.elementAt(i);
					setProgress("Update  "+p.getTitle());
					p.update(true);
				}

				setProgress("Stop simulation");
				emulation(false);
				messages(cpu.toString());
				setProgress("Garbage collection");
				System.gc();

				}
				catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex)
				{
					messages(ex);
					ex.printStackTrace(System.out);
				}
				
			}
				
		};
		w.start();
	}

	private void erase()
	{
		for (int i = 0 ; i < cpu.getCodeSize() ; i++){
			cpu.code(i,0xff);
                }
	}
	
	private void reset()
	{

		cpu.reset();
		try
		{
			cpu.pc(info.reset.getValue());
		} catch (Exception ex) {
		}
					
		minCpuUsage = 100;
		maxCpuUsage = 0;
		cpuTime = 0;
		avgCpuUsage = 0;
		
	}
	
	public void setCpu(MCS51 cpu)
	{
		info.reset.setValue(0);
		
		this.cpu = cpu;
		System.out.println("Reset");
		reset();
		
		for (int i = 0 ; i < panels.size() ; i++)
		{
			System.out.println("SetCpu"+panels.elementAt(i));
			((J51Panel)panels.elementAt(i)).setCpu(cpu);
		}

		cpu.addPerformanceListener(this);
		
		if (isVisible())
		{
			invalidate();
			pack();
		}

		System.out.println("Update Panel");
		updatePanel(true);
		emulation(false);
		messages(cpu.toString());
	}
	
	void addKey(JMenuItem item,char m)
	{
		item.setMnemonic(m);
		item.setAccelerator(KeyStroke.getKeyStroke(m, KeyEvent.ALT_MASK));
	}

	public void messages(Throwable ex)
	{
		if (!(ex instanceof InterruptedException))
		{
			java.util.logging.Logger.getLogger(J51.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		String msg = ex.getMessage();
		if (msg == null){
			msg = ex.toString();
                }
		messages(msg);
	}

	class UpdateMessage implements Runnable
	{
		private String msg;

		public UpdateMessage(String msg)
		{
			this.msg = msg;
			SwingUtilities.invokeLater(this);

		}

                @Override
		public void run()
		{
			if (msg == null){
				msg = "";
                        }
			messages.setText(msg);
		}
	}

	private void messages(String msg)
	{
		new UpdateMessage(msg);
	}
	
	private void emulation(boolean mode)
	{
		if (cpu != null){
			cpu.setEmulation(mode);
                }
		menuCpu.setEnabled(!mode);
		actionDebugErase.setEnabled(!mode);
		actionDebugStop.setEnabled(mode);
		actionDebugReset.setEnabled(!mode);
		actionDebugGo.setEnabled(!mode);
		actionDebugTrace.setEnabled(!mode);
		actionDebugStep.setEnabled(!mode);
		actionFileLoad.setEnabled(!mode);
		actionFileExit.setEnabled(!mode);
		actionToolsStatistics.setEnabled(!mode);
		actionToolsProfile.setEnabled(!mode);
		actionToolsInterrupt.setEnabled(!mode);
		
		for (int i = 0 ; i < panels.size() ; i++)
		{
			((J51Panel)panels.elementAt(i)).setEmulation(mode);
		}

		updatePanel(false);
	}

	private void createMenuCpuLine(JMenu m,ButtonGroup g,String name)
	{
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(name, false);
		
		item.addActionListener((ActionEvent ae) -> {
                    setCpu(ae.getActionCommand());
                });

		g.add(item);
		m.add(item);

		if (first == null)
			first = item;
	}


	/**
	 * Load one icon from the resource.
	 *
	 * @sice 1.04
	 */
	public Icon getIcon(String name)
	{
		log.log(Level.FINER, "Loading {0}", name);
		URL url = J51.class.getResource("images/" + name);
		
		if (url == null)
		{
			log.log(Level.INFO, "ImageFactory.getImageIcon - not found: {0}", name);
			return null;
		}
		
		Icon  icon =  new ImageIcon(url);

		return icon;

	}
	
	/**
	 * Add one icon to JMenu or JMenuItem.
	 *
	 * @since 1.04
	 */
	void addIcon(JMenu menu,String name)
	{
		menu.setIcon(getIcon(name));
	}

	void addIcon(Action action,String name)
	{
		Icon icon = getIcon(name);
		if (icon != null)
			action.putValue(Action.SMALL_ICON,icon);
	}
	
	JMenu createMenuCpu()
	{
		menuCpu = new JMenu("Cpu",true);
		addIcon(menuCpu,"cpu.gif");
		ButtonGroup buttonGroup = new ButtonGroup();

		// Add cpu selection 
		try
		{
			BufferedReader rd = new BufferedReader(new FileReader("/home/spy/Source/8051/j51.conf"));
			String line;

			while ((line = rd.readLine()) != null)
			{
				if (line.startsWith("#"))
					continue;
				if (line.length() < 3)
					continue;
				createMenuCpuLine(menuCpu,buttonGroup,line);

			}
			rd.close();
		} catch (IOException ex) {
                    java.util.logging.Logger.getLogger(J51.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (menuCpu.getMenuComponentCount() == 0){
			createMenuCpuLine(menuCpu,buttonGroup,"j51.intel.P8051");
                }


		return menuCpu;
	}
	
	JMenu createMenuLaf()
	{
		JMenu lnf = new JMenu("Look & Feel", true);
		addIcon(lnf,"laf.gif");
		
		ButtonGroup buttonGroup = new ButtonGroup();
		final UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
		
            for (UIManager.LookAndFeelInfo info1 : info) {
                boolean set = false;
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(info1.getName(), set);
                final String className = info1.getClassName();
                item.addActionListener((ActionEvent ae) -> {
                    try
                    {
                        UIManager.setLookAndFeel(className);
                    }
                    catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e)
                    {
                        messages(e);
                    }
                    SwingUtilities.updateComponentTreeUI(J51.this);
                    J51.this.pack();
                });
                buttonGroup.add(item);
                lnf.add(item);
            }

		lnf.setMnemonic('K');

		return lnf;
		
	}

	JButton addToBar(JToolBar bar,Action action)
	{
		JButton b = bar.add(action);
		String s = (String)action.getValue(Action.NAME);
		if (s != null)
			b.setToolTipText(s);

		return b;
	}
	
	void createMenuBar()
	{
		JMenuBar bar = new JMenuBar();
		bar.add(createMenuFile());
		bar.add(createMenuCpu());
		bar.add(createMenuTools());
		bar.add(createMenuDebug());
		bar.add(createMenuLaf());
		setJMenuBar(bar);
		emulation(false);

		toolBar = new JToolBar();
		addToBar(toolBar,actionDebugGo);
		addToBar(toolBar,actionDebugTrace);
		addToBar(toolBar,actionDebugStep);
		buttonStop = addToBar(toolBar,actionDebugStop);
		addToBar(toolBar,actionDebugReset);
		addToBar(toolBar,actionDebugErase);
		addToBar(toolBar,actionFileLoad);

	}

        @Override
	public void cpuPerformance(int cpu,int elapsed)
	{
		avgCpuUsage = (cpu + avgCpuUsage) / 2;
		cpuTime += elapsed;
		if (cpuTime  > 0){
			if (cpu < minCpuUsage)
				minCpuUsage = cpu;
			if (cpu > maxCpuUsage)
				maxCpuUsage = cpu;

			messages("CPU Usage " + cpu + "%, min " + minCpuUsage + "%, max " + maxCpuUsage + "%, avg " + avgCpuUsage + "%, run " + cpuTime / 1000 + " sec.");
			SwingUtilities.invokeLater(() -> {
                            info.updateClock();
                        });
		}
	}


	private void load(String name) throws Exception
	{
		BufferedReader rd = new BufferedReader(new FileReader(name));
		String line;
		int start = 0x10000;
		int end = 0;
		
		while ((line = rd.readLine()) != null)
		{
			if (!line.startsWith(":"))
				throw new Exception(name + " is not a valid intel file");

			
			int lenData = Hex.getByte(line, 1);
			int address	= Hex.getWord(line, 3);
			int type	= Hex.getByte(line, 7);

			int chksum = lenData + address / 256 + address + type;
			
			for (int i = 0 ; i < lenData + 1; i++)
				chksum += Hex.getByte(line, 9 + i * 2);
			chksum &= 0xff;

			if (chksum != 0)
				throw new Exception("Invalid chksum " + Hex.bin2byte(chksum) + " in " + line);

			if (type == 1)
				break;
			if (type == 3)
				continue;
			
			if (type != 0)
				throw new Exception("Unsupported record type " + type);

			if (address < start)
				start = address;
			if (address + lenData - 1 > end)
				end = address + lenData - 1;
			for (int i = 0 ; i < lenData ; i++)
				cpu.code(address + i, Hex.getByte(line, 9 + i * 2));
		}
		messages(" loaded at " + Hex.bin2word(start) + "-" + Hex.bin2word(end));
		
		rd.close();

		int pos = name.indexOf('.');
		if (pos != -1)
		{
			name = name.substring(0,pos)+".map";
		}

		try
		{
		rd = new BufferedReader(new FileReader(name));

		while ((line = rd.readLine()) != null)
		{
			line = line.trim();
			if (line.startsWith("0C:"))
			{
				int address = Hex.getWord(line,3);
				String label = line.substring(7);
				label = label.trim();
				cpu.setCodeName(address,label);
			}
			
		}

		rd.close();
		} catch (Exception ex) {
                    java.util.logging.Logger.getLogger(J51.class.getName()).log(Level.SEVERE, null, ex);
		}
		
	}
	
	JMenu createMenuFile()
	{
		actionFileLoad = new AbstractAction("Load")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(J51.this) == JFileChooser.APPROVE_OPTION)
					{
						load(fc.getSelectedFile().getCanonicalPath());
						updatePanel(true);
					}
				} catch (Exception ex) {
					messages(ex);
				}
			}
		};
		addIcon(actionFileLoad,"load.gif");
		
		actionFileExit = new AbstractAction("Exit")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		};
		addIcon(actionFileExit,"exit.gif");
		
		JMenu menu = new JMenu("File");
		addIcon(menu,"file.gif");
		
		addKey(menu.add(actionFileLoad),'L');
		addKey(menu.add(actionFileExit),'X');

		menu.setMnemonic('F');
		
		return menu;
	}

	void performTree(String title,JTree tree)
	{
		class MyRenderer extends  DefaultTreeCellRenderer
		{
                        @Override
			public Component getTreeCellRendererComponent(JTree tree,
				Object value,
				boolean selected,
				boolean expanded,
				boolean leaf,
				int row,
				boolean hasFocus)
			{
				super.getTreeCellRendererComponent(tree,value,selected,expanded,leaf,row,hasFocus);
				
				Font font = getFont();
				
				if (font != null)
				{
					font = new Font("Monospaced",font.getStyle(),font.getSize());
					setFont(font);
				}

				return this;
			}
			
		};

		tree.setCellRenderer(new MyRenderer());
		Dimension size = getPreferredSize();
		size.width = size.width * 2 / 3;
		size.height = size.height * 2 / 3;
		
		JScrollPane sc = new JScrollPane(tree);
		
		sc.setPreferredSize(size);
		JDialog d = new JDialog(this,title,true);
		Point p = getLocation();
		p.x += size.width / 10;
		p.y += size.height / 10;
		d.setLocation(p);
		d.setContentPane(sc);
		d.pack();
		d.setVisible(true);
	}


	void performInterrupt()
	{
		SortedLong sl = new SortedLong();
		
		for (int i = 0 ; i < cpu.getInterruptCount() ; i++)
		{
			InterruptStatistic is = cpu.getInterruptAt(i);
			long counter = is.getCounter();
			sl.put(counter,is.toString());

		}

		performTree("Interrupt",sl.createTree());

	}

	void performProfile()
	{
		SortedLong sl = new SortedLong();
		for (int i = 0 ; i < cpu.getCodeSize() ; i++)
		{
			long counter = cpu.getExecutionCounter(i);
			if (counter > 0)
			{
				sl.put(counter,cpu.getDecodeAt(i));
			}
			
		}
		
		performTree("Profiling",sl.createTree());
		
	}

	
	void performStatistics()
	{
		SortedLong sl = new SortedLong();
		for (int i = 0 ; i < 256 ; i ++)
		{
			long counter = cpu.getOpcodeCounter(i);
			if (counter > 0)
			{
				sl.put(counter,cpu.getOpcodeDescription(i));
			}
		}

		performTree("Statistics",sl.createTree());
	}
	
	JMenu createMenuTools()
	{
		actionToolsProfile = new AbstractAction("Profile")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				performProfile();
			}
			
		};
		
		actionToolsStatistics = new AbstractAction("Statistics")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				performStatistics();
			}
		};

		actionToolsInterrupt = new AbstractAction("Interrupt")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				performInterrupt();
			}
		};


		JMenu menu = new JMenu("Tools");
		addIcon(menu,"tools.gif");
		
		addKey(menu.add(actionToolsProfile),'P');
		menu.add(actionToolsInterrupt);
		addKey(menu.add(actionToolsStatistics),'C');

		menu.setMnemonic('T');

		return menu;
	}

	
	JMenu createMenuDebug()
	{
		actionDebugErase = new AbstractAction("Erase")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				erase();
				updatePanel(true);
			}
		};
		addIcon(actionDebugErase,"erase.gif");
		
		actionDebugReset = new AbstractAction("Reset")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				reset();
				updatePanel(false);
			}
		};
		addIcon(actionDebugReset,"reset.gif");
		
		actionDebugStop = new AbstractAction("Stop")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					thread.interrupt();
				}
				catch (Exception ex)
				{
			java.util.logging.Logger.getLogger(J51.class.getName()).log(Level.SEVERE, null, ex);
				}
				
			}
		};

		addIcon(actionDebugStop,"stop.gif");
		
		actionDebugTrace = new AbstractAction("Step into")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.step();
					updatePanel(false);
				}
				catch (Exception ex)
				{
					messages(ex);
				}
			}
		};
		addIcon(actionDebugTrace,"stepinto.gif");
		
		actionDebugGo = new AbstractAction("Go")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				thread = new Thread(() -> {
                                    messages("Simulating ....");
                                    try
                                    {
                                        cpu.go(-1);
                                    }
                                    catch (Exception ex)
                                    {
                                        messages(ex);
                                    }
                                    
                                    
                                    SwingUtilities.invokeLater(() -> {
                                        emulation(false);
                                    });
                                });

				emulation(true);

				
				
				thread.start();
			}

			
		};
		addIcon(actionDebugGo,"play.gif");
		
		actionDebugStep = new AbstractAction("Step over")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				thread = new Thread(() -> {
                                    messages("Emulating ....");
                                    try
                                    {
                                        cpu.pass();
                                    }
                                    catch (Exception ex)
                                    {
                                        messages(ex);
                                    }
                                    
                                    SwingUtilities.invokeLater(() -> {
                                        emulation(false);
                                    });
                                });

				emulation(true);

				thread.start();
			}


		};
		addIcon(actionDebugStep,"step.gif");
		
		JMenu menu = new JMenu("Debug");
		addIcon(menu,"debug.gif");
		
		addKey(menu.add(actionDebugTrace),'I');
		addKey(menu.add(actionDebugStep),'O');
		addKey(menu.add(actionDebugReset),'R');
		addKey(menu.add(actionDebugGo),'G');
		addKey(menu.add(actionDebugStop),'S');
		addKey(menu.add(actionDebugErase),'E');
		
		menu.setMnemonic('D');


		return menu;
	}
	
	static public void main(String argv[])
	{
		
		try
		{
			J51 j51 = new J51();
			j51.updatePanel(true);
			j51.pack();
			j51.setVisible(true);
			j51.requestFocus();
		} catch (Exception ex) {
			 java.util.logging.Logger.getLogger(J51.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		}
	}

	public void updatePanel(boolean force)
	{
		if (cpu == null){
			return;
                }
		
		for (int i = 0 ; i < panels.size() ; i++)
		{
			((J51Panel)panels.elementAt(i)).update(force);
		}

	}
}

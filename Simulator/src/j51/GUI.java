/**
 * $Id: J51.java 70 2010-07-01 09:57:00Z mviara $
 */
package j51;

import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.tree.*;

import j51.util.*;
import j51.intel.*;
import j51.swing.*;
import jx.classfile.ClassData;

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
public class GUI extends JFrame implements MCS51Performance, ActionListener
{
	private static final Logger log = Logger.getLogger(GUI.class);
	
	static  private GUI	instance = null;
	private final J51Panel	peripheral;
	private final JRegister	register;
	private final JAssembly	assembly;
	private final JSfr	sfr;
	private final JIdata	idata;
	private final JXdata	xdata;
	private final JCode	code;
	private final JInfo     info;
	private final JFixedField messages;
	private JToolBar toolBar = new JToolBar();
	private MCS51 cpu;
	private JFileChooser   fc = null;
	private JButton        buttonStop;
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
	private final java.util.ArrayList panels = new java.util.ArrayList();


	GUI(){
		setTitle("J51 1.05 $Revision: 70 $ - Created by mario@viara.eu");
		pack();
		setVisible(true);
		info	   = new JInfo();
		register   = new JRegister();
		assembly   = new JAssembly();
		sfr	   = new JSfr();
		idata	   = new JIdata();
		xdata	   = new JXdata();
		code	   = new JCode();
		peripheral = new JPeripheral();
		messages   = new JFixedField(64);
		JFactory.setTitle(messages,"Messages");
		JPanel p = new JPanel(new GridBagLayout());
		createMenuBar();
		register.setChangeListener(this);
		
		GridBagConstraints g = new GridBagConstraints();
		g.insets = new Insets(1, 1, 1, 1);
		g.gridx = 0;
                g.gridy = 0;
                g.gridwidth = 1;
                g.gridheight = 1;

		g.fill = GridBagConstraints.NONE;
		g.anchor = GridBagConstraints.WEST;
		g.gridwidth = 2;
		p.add(toolBar,g);
		
		g.gridy++;
                g.fill = GridBagConstraints.BOTH;
                g.anchor = GridBagConstraints.CENTER;
		
		g.gridwidth = 2;
		p.add(info, g);
		g.gridwidth = 1;
                g.gridy++;
		p.add(register,g);
		g.gridx++;
		
		JTabbedPane tp = new JTabbedPane();
		tp.add("Assembler", assembly);
		tp.add("SFR", sfr);
		tp.add("IDATA", idata);
		tp.add("XDATA", xdata);
		tp.add("CODE", code);
		
		p.add(tp, g);
		g.gridx = 0;
                g.gridy++;
                g.gridwidth = 2;
		p.add(messages, g);

		g.gridx = 2;
                g.gridy = 0;
		g.gridheight = 4;
                g.gridwidth = 1;
		p.add(peripheral, g);
		
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

	static public GUI getInstance()
	{
		return instance;
	}
	

        @Override
	public void actionPerformed(ActionEvent e)
	{
		String source = e.getActionCommand();
		
		
		if (source.equals("PC")){
			assembly.update(false);
		}

		if (source.equals("SFR")){
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
				try{
					Class c = Class.forName(name);
					setProgress("Loading class");
					MCS51 newCpu = (MCS51)c.newInstance();
					info.reset.setValue(0);
					GUI.this.cpu = newCpu;
					setProgress("Reset cpu");
					reset();

				for (int i = 0 ; i < panels.size() ; i++){
					J51Panel p = (J51Panel)panels.get(i);
					setProgress("Initialize  " + p.getTitle());
					p.setCpu(cpu);
				}

				cpu.addPerformanceListener(GUI.this);

				if (GUI.this.isVisible()){
					GUI.this.invalidate();
					GUI.this.pack();
				}

				for (int i = 0 ; i < panels.size() ; i++){
					J51Panel p = (J51Panel)panels.get(i);
					setProgress("Update  " + p.getTitle());
					p.update(true);
				}

				setProgress("Stop simulation");
				emulation(false);
				messages(cpu.toString());
				setProgress("Garbage collection");
				System.gc();
				} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
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
			cpu.code(i, 0xff);
                }
	}
	
	private void reset()
	{

		cpu.reset();
		try{
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
		
		for (int i = 0 ; i < panels.size() ; i++){
			System.out.println("SetCpu" + panels.get(i));
			((J51Panel)panels.get(i)).setCpu(cpu);
		}

		cpu.addPerformanceListener(this);
		
		if (isVisible()){
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
		if (!(ex instanceof InterruptedException)){
			java.util.logging.Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
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
			((J51Panel)panels.get(i)).setEmulation(mode);
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

		if (first == null){
			first = item;
                }
	}


	/**
	 * Load one icon from the resource.
	 *
	 * @sice 1.04
	 */
	public Icon getIcon(String name)
	{
		log.log(Level.FINER, "Loading {0}", name);
		URL url = GUI.class.getResource("images/" + name);
		
		if (url == null){
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
		menuCpu = new JMenu("CPU",true);
		addIcon(menuCpu,"cpu.gif");
		ButtonGroup buttonGroup = new ButtonGroup();

		// Add cpu selection 
		try{
			BufferedReader rd = new BufferedReader(new FileReader("./j51.conf"));
			String line;

			while ((line = rd.readLine()) != null){
				if (line.startsWith("#"))
					continue;
				if (line.length() < 3)
					continue;
				createMenuCpuLine(menuCpu, buttonGroup, line);
			}
			rd.close();
		} catch (IOException ex) {
                    java.util.logging.Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (menuCpu.getMenuComponentCount() == 0){
			createMenuCpuLine(menuCpu,buttonGroup, "j51.intel.P8051");
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
                    try{
                        UIManager.setLookAndFeel(className);
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                        messages(e);
                    }
                    SwingUtilities.updateComponentTreeUI(GUI.this);
                    GUI.this.pack();
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
		
		while ((line = rd.readLine()) != null){
			if (!line.startsWith(":")){
				throw new Exception(name + " is not a valid intel file");
                        }
			
			int lenData = Hex.getByte(line, 1);
			int address = Hex.getWord(line, 3);
			int type    = Hex.getByte(line, 7);

			int chksum = lenData + address / 256 + address + type;
			
			for (int i = 0 ; i < lenData + 1; i++){
				chksum += Hex.getByte(line, 9 + i * 2);
                        }
			chksum &= 0xff;

			if (chksum != 0){
				throw new Exception("Invalid chksum " + Hex.bin2byte(chksum) + " in " + line);
                        }

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
			for (int i = 0 ; i < lenData ; i++){
				cpu.code(address + i, Hex.getByte(line, 9 + i * 2));
                        }
		}
		messages(" loaded at " + Hex.bin2word(start) + "-" + Hex.bin2word(end));
		
		rd.close();

		int pos = name.indexOf('.');
		if (pos != -1){
			name = name.substring(0, pos) + ".map";
		}

		try{
                    rd = new BufferedReader(new FileReader(name));

                    while ((line = rd.readLine()) != null){
			line = line.trim();
			if (line.startsWith("0C:")){
				int address = Hex.getWord(line, 3);
				String label = line.substring(7);
				label = label.trim();
				cpu.setCodeName(address,label);
			}
                    }

                    rd.close();
		} catch (Exception ex) {
                    java.util.logging.Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
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
					if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
					{
                                            if(fc.getName().endsWith("hex")){
						load(fc.getSelectedFile().getCanonicalPath());
                                            } else if(fc.getName().endsWith("bin")){
                                                File file = new File(fc.getSelectedFile().getCanonicalPath());
                                            } else if(fc.getName().endsWith("class")){
                                                File file = new File(fc.getSelectedFile().getCanonicalPath());
                                                InputStream is = new FileInputStream(file);
                                                ClassData data = new ClassData(new DataInputStream(is));
                                            }
                                            updatePanel(true);
					}
				} catch (Exception ex) {
					messages(ex);
				}
			}
		};
		addIcon(actionFileLoad, "load.gif");
		
		actionFileExit = new AbstractAction("Exit")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		};
		addIcon(actionFileExit, "exit.gif");
		
		JMenu menu = new JMenu("File");
		addIcon(menu, "file.gif");
		
		addKey(menu.add(actionFileLoad), 'L');
		addKey(menu.add(actionFileExit), 'X');

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
				super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
				
				Font font = getFont();
				
				if (font != null)
				{
					font = new Font("Monospaced", font.getStyle(), font.getSize());
					setFont(font);
				}

				return this;
			}
			
		}

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

		performTree("Interrupt", sl.createTree());

	}

	void performProfile()
	{
		SortedLong sl = new SortedLong();
		for (int i = 0 ; i < cpu.getCodeSize() ; i++)
		{
			long counter = cpu.getExecutionCounter(i);
			if (counter > 0)
			{
				sl.put(counter, cpu.getDecodeAt(i));
			}
		}
		
		performTree("Profiling", sl.createTree());
	}

	
	void performStatistics()
	{
		SortedLong sl = new SortedLong();
		for (int i = 0 ; i < 256 ; i ++)
		{
			long counter = cpu.getOpcodeCounter(i);
			if (counter > 0)
			{
				sl.put(counter, cpu.getOpcodeDescription(i));
			}
		}

		performTree("Statistics", sl.createTree());
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
		addIcon(menu, "tools.gif");
		
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
				} catch (Exception ex) {
			java.util.logging.Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
				}
				
			}
		};

		addIcon(actionDebugStop, "stop.gif");
		
		actionDebugTrace = new AbstractAction("Step into")
		{
                        @Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					cpu.step();
					updatePanel(false);
				} catch (Exception ex) {
					messages(ex);
				}
			}
		};
		addIcon(actionDebugTrace, "stepinto.gif");
		
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
                                    } catch (Exception ex) {
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
                                    } catch (Exception ex) {
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
		try {
			GUI j51 = new GUI();
			j51.updatePanel(true);
			j51.pack();
			j51.setVisible(true);
			j51.requestFocus();
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		}
	}

	public void updatePanel(boolean force)
	{
		if (cpu == null){
			return;
                }
		
		for (int i = 0 ; i < panels.size() ; i++){
			((J51Panel)panels.get(i)).update(force);
		}
	}
}

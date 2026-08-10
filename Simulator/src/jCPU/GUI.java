/**
 * $Id: J51.java 70 2010-07-01 09:57:00Z mviara $
 */
package jCPU;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.tree.*;
import assets.Assets;
import j51.swing.J51Panel;

import j51.util.*;
import j51.swing.*;

import java.awt.datatransfer.StringSelection;
import jCPU.JVM.ByteCode;
import jCPU.MCS51.CPU;
import jCPU.MCS51.MCS51Performance;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import jx.classfile.ClassData;
import jx.classfile.MethodData;
import jx.compiler.persistent.CodeFile;
import jx.compiler.persistent.CompiledClass;
import jx.compiler.persistent.CompiledMethod;
import jx.compiler.persistent.ExtendedDataInputStream;

import nl.lxtreme.arm.memory.Chunk;
import nl.lxtreme.arm.memory.Memory;
import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.ProgramHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * J51 main Frame.
 * 
 * @author Mario Viara
 * @version 1.01
 *
 * 1.01 Added support for menu icon.
 * 
 */
public class GUI extends JFrame implements MCS51Performance, ActionListener
{
	private static final Logger log = Logger.getLogger(GUI.class);
	private static GUI instance = null;
	private final J51Panel peripheral;
	private final JRegister register;
        private final JAssembly assembly;
	private final JSfr sfr;
	private final JIdata idata;
	private final JXdata xdata;
	private final JCode code;
	private final JInfo info;
	private final JTextArea messages;

	private JToolBar toolBar = new JToolBar();
	private iCPU cpu;
	private JFileChooser fc = null;
	private JButton buttonStop;
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
	private final java.util.ArrayList<J51Panel> panels = new java.util.ArrayList<>();
	private final Assets assets = new Assets();
	private JSplitPane mainSplit;
	private JSplitPane leftSplit;
	private JSplitPane bottomSplit;

	GUI(){
		setTitle("J51 1.05 $Revision: 70 $ - Created by mario@viara.eu");

		info = new JInfo();
		register = new JRegister();
		assembly = new JAssembly();
		sfr = new JSfr();
		idata = new JIdata();
		xdata = new JXdata();
		code = new JCode();
		peripheral = new JPeripheral();
		messages = new JTextArea(5, 40);
		messages.setEditable(false);
		messages.setFont(new Font("Monospaced", Font.PLAIN, 12));

		createMenuBar();
		register.setChangeListener(this);

		JTabbedPane tp = new JTabbedPane();
		tp.add("SFR", sfr);
		tp.add("IDATA", idata);
		tp.add("XDATA", xdata);
		tp.add("CODE", code);

		bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, assembly, tp);
		bottomSplit.setResizeWeight(0.5);
		bottomSplit.setOneTouchExpandable(true);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(register, BorderLayout.NORTH);
		leftPanel.add(bottomSplit, BorderLayout.CENTER);

		JScrollPane peripheralScroll = new JScrollPane(peripheral);
		peripheralScroll.setBorder(BorderFactory.createTitledBorder("Peripheral"));

		mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, peripheralScroll);
		mainSplit.setResizeWeight(0.6);
		mainSplit.setOneTouchExpandable(true);

		JScrollPane messagesScroll = new JScrollPane(messages);
		messagesScroll.setBorder(BorderFactory.createTitledBorder("Messages"));

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);
		add(mainSplit, BorderLayout.CENTER);
		add(messagesScroll, BorderLayout.SOUTH);

		panels.add(info);
		panels.add(assembly);
		panels.add(sfr);
		panels.add(idata);
		panels.add(xdata);
		panels.add(code);
		panels.add(register);
		panels.add(peripheral);

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

		Worker w = new Worker(this, "Setup simulator", "Loading")
		{
			@Override
			public void process()
			{
				try{
					Class c = Class.forName(name);
					setProgress("Loading class");
					iCPU newCpu = (iCPU)c.newInstance();
					info.reset.setValue(0);
					GUI.this.cpu = newCpu;
					setProgress("Reset cpu");
					reset();

					for (int i = 0 ; i < panels.size() ; i++){
						J51Panel p = panels.get(i);
						setProgress("Initialize  " + p.getTitle());
						p.setCpu(cpu);
					}

					cpu.addPerformanceListener(GUI.this);

					setProgress("Stop simulation");
					emulation(false);
					messages(cpu.toString());
					setProgress("Garbage collection");
					System.gc();
				} catch (ClassNotFoundException ex) {
					showError("Failed to load " + name + " simulator: class not found. Check j51.conf.", ex);
				} catch (InstantiationException | IllegalAccessException ex) {
					showError("Failed to instantiate " + name + ": " + ex.getMessage() +
							". Ensure class implements iCPU and has public no-arg constructor.", ex);
				} catch (Exception ex) {
					showError("Failed to initialize " + name + ": " + ex.getMessage(), ex);
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

	public void setCpu(CPU cpu)
	{
		info.reset.setValue(0);

		this.cpu = cpu;
		System.out.println("Reset");
		reset();

		for (int i = 0 ; i < panels.size() ; i++){
			System.out.println("SetCpu" + panels.get(i));
			panels.get(i).setCpu(cpu);
		}

		cpu.addPerformanceListener(this);

		updatePanel(true);
		emulation(false);
		messages(cpu.toString());
	}

	void addKey(JMenuItem item, int key)
	{
		item.setAccelerator(KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
	}

	public void messages(Throwable ex)
	{
		if (!(ex instanceof InterruptedException)){
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		}

		String msg = ex.getMessage();
		if (msg == null){
			msg = ex.toString();
		}
		messages(msg);
	}

	private void messages(String msg)
	{
		SwingUtilities.invokeLater(() -> {
			messages.append(msg + "\n");
			messages.setCaretPosition(messages.getDocument().getLength());
		});
	}

	private void showError(String userMessage, Exception ex) {
		messages("[ERROR] " + userMessage);
		if (ex != null) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, userMessage, ex);
			ex.printStackTrace(System.out);
		}
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
			panels.get(i).setEmulation(mode);
		}

		updatePanel(false);
	}

	private void createMenuCpuLine(JMenu m, ButtonGroup g, String name)
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

	public Icon getIcon(String name)
	{
		log.log(Level.FINER, "Loading {0}", name);
		Icon icon = assets.getIcon("/assets/images/" + name);

		if (icon == null){
			log.log(Level.INFO, "ImageFactory.getImageIcon - not found: {0}", name);
			return null;
		}

		return icon;
	}

	void addIcon(JMenu menu, String name)
	{
		menu.setIcon(getIcon(name));
	}

	void addIcon(Action action, String name)
	{
		Icon icon = getIcon(name);
		if (icon != null)
			action.putValue(Action.SMALL_ICON, icon);
	}

	private java.util.List<String> readCpuConfig()
	{
		java.util.List<String> cpus = new java.util.ArrayList<>();
                try (BufferedReader rd = new BufferedReader(new FileReader("j51.conf"))) {
                    String line;
                    while ((line = rd.readLine()) != null){
                        if (line.startsWith("#"))
                            continue;
                        if (line.length() < 3)
                            continue;
                        cpus.add(line);
                    }
		} catch (IOException ex) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (cpus.isEmpty()){
			cpus.add("j51.intel.P8051");
		}
		return cpus;
	}

	private void writeCpuConfig(java.util.List<String> cpus)
	{
		try {
			PrintWriter pw = new PrintWriter(new FileWriter("j51.conf"));
			for (String cpu : cpus){
				pw.println(cpu);
			}
			pw.close();
		} catch (IOException ex) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	JMenu createMenuCpu()
	{
		menuCpu = new JMenu("CPU", true);
		addIcon(menuCpu, "cpu.gif");
		ButtonGroup buttonGroup = new ButtonGroup();

		java.util.List<String> cpus = readCpuConfig();
		for (String cpu : cpus){
			createMenuCpuLine(menuCpu, buttonGroup, cpu);
		}

		return menuCpu;
	}

	JMenu createMenuLaf()
	{
		JMenu lnf = new JMenu("Look & Feel", true);
		addIcon(lnf, "laf.gif");

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

	JButton addToBar(JToolBar bar, Action action)
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
		addToBar(toolBar, actionDebugGo);
		addToBar(toolBar, actionDebugTrace);
		addToBar(toolBar, actionDebugStep);
		buttonStop = addToBar(toolBar, actionDebugStop);
		addToBar(toolBar, actionDebugReset);
		addToBar(toolBar, actionDebugErase);
		addToBar(toolBar, actionFileLoad);
	}

	@Override
	public void cpuPerformance(int cpu, int elapsed)
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

	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
		String content = new String(Files.readAllBytes(Paths.get(filename)));
		return new JSONObject(content);
	}

	private void loadHex(String name) throws Exception
	{
		try {
			BufferedReader rd;
			java.util.List<String> lines = new java.util.ArrayList<>();

			if(name.endsWith("json")) {
				JSONObject object = parseJSONFile(name);
				Iterator<String> it = object.keys();
				java.util.List array = ((JSONArray)object.get("demo.main")).toList();
				for(Object o : array){
					lines.add(":" + (String)o);
				}
			} else {
				rd = new BufferedReader(new FileReader(name));
				String line;
				while((line = rd.readLine()) != null){
					lines.add(line);
				}
				rd.close();
			}

			int start = 0x10000;
			int end = 0;

			for (String line : lines){
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
		} catch (Exception ex) {
			throw new Exception("Failed to load " + name + " as Intel HEX: " + ex.getMessage(), ex);
		}

		int pos = name.indexOf('.');
		if (pos != -1){
			name = name.substring(0, pos) + ".map";
		}

                try (BufferedReader mapRd = new BufferedReader(new FileReader(name))) {
                    String line;
                    while ((line = mapRd.readLine()) != null){
                        line = line.trim();
                        if (line.startsWith("0C:")){
                            int address = Hex.getWord(line, 3);
                            String label = line.substring(7);
                            label = label.trim();
                            cpu.setCodeName(address, label);
                        }
                    }
		} catch (Exception ex) {
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void loadBin(String path) throws Exception
	{
		try {
			File file = new File(path);
			Elf elf = new Elf(file);
			Memory m = new Memory();
			for (ProgramHeader ph : elf.programHeaders){
				int size = (int) ph.segmentMemorySize;
				if (size <= 0){
					continue;
				}
				Chunk chunk = m.create(ph.virtualAddress, size);
				chunk.data = elf.getSegment(ph);
			}
			for(int i = 0; i < 0x10000; i++){
				cpu.code(i, m.read((int)(i + elf.header.entryPoint)));
			}
		} catch (IOException ex) {
			throw new IOException("Invalid ELF binary " + path + ": " + ex.getMessage(), ex);
		}
	}

	private void loadRawBin(String path) throws Exception
	{
		try {
			File file = new File(path);
			FileInputStream fis = new FileInputStream(file);
			byte[] code = fis.readAllBytes();
			for(int i = 0; i < code.length; i++){
				cpu.code(i, code[i + 0x1000 * 0]);
				if(i == 0x10000 - 1) break;
			}
		} catch (IOException ex) {
			throw new IOException("Failed to load raw binary " + path + ": " + ex.getMessage(), ex);
		}
	}

	private void loadClass(String path) throws Exception
	{
		try {
			File file = new File(path);
			InputStream is = new FileInputStream(file);
			ClassData data = new ClassData(new DataInputStream(is));
			ByteCode.cp = data.getConstantPool();
			for(MethodData method:data.getMethodData()){
				if("main".equals(method.getName())){
					byte[] code = method.getCode().getBytecode();
					for(int i = 0; i < code.length; i++){
						cpu.code(i, code[i]);
					}
				}
			}
		} catch (IOException ex) {
			throw new Exception("Failed to load Java class " + path + ": " + ex.getMessage(), ex);
		}
	}

	private void loadJar(String path) throws Exception
	{
		try {
			JarFile jar = new JarFile(path);
			Enumeration<JarEntry> entries = jar.entries();
			String main = null;
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if(name.equals("META-INF/MANIFEST.MF")){
					try (InputStream is = jar.getInputStream(entry)) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						while(reader.ready()){
							String line = reader.readLine();
							if(line.startsWith("Main-Class")){
								main = line.split(":")[1].strip().replace(".", "/") + ".class";
							}
						}
					}
				}
				if (main != null){
					if(name.endsWith(main)){
						ClassData data = new ClassData(new DataInputStream(jar.getInputStream(entry)));
						ByteCode.cp = data.getConstantPool();
						for(MethodData method:data.getMethodData()){
							if("main".equals(method.getName())){
								byte[] code = method.getCode().getBytecode();
								for(int i = 0; i < code.length; i++){
									cpu.code(i, code[i]);
								}
							}
						}
						break;
					}
				}
			}
		} catch (IOException ex) {
			throw new Exception("Failed to load JAR " + path + ": " + ex.getMessage(), ex);
		}
	}

	private void loadJll(String path) throws Exception
	{
		try {
			ExtendedDataInputStream stream = new ExtendedDataInputStream(new FileInputStream(path));
			CodeFile file = new CodeFile(null, null);
			java.util.ArrayList<CompiledClass> allClasses = file.read(stream);
			file.size();
			mainloop: for(CompiledClass clazz:allClasses){
				for(CompiledMethod method:clazz.getMethods()){
					byte[] code = method.getCode();
					if(code == null) continue;
					for(int i = 0; i < code.length; i++){
						cpu.code(i, code[i]);
					}
					break mainloop;
				}
			}
		} catch (Exception ex) {
			throw new Exception("Failed to load JLL file " + path + ": " + ex.getMessage(), ex);
		}
	}

	private void performFileOpen()
	{
		try{
			if (fc == null)
			{
				fc = new JFileChooser();
				fc.setCurrentDirectory(new File("."));
			}
			if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
			{
				String path = fc.getSelectedFile().getCanonicalPath();
				tryLoadFile(path);
				updatePanel(true);
			}
		} catch (FileNotFoundException ex) {
			showError("File not found: " + ex.getMessage(), ex);
		} catch (IllegalArgumentException ex) {
			showError(ex.getMessage(), ex);
		} catch (Exception ex) {
			showError("Failed to load file: " + ex.getMessage(), ex);
		}
	}

	private void tryLoadFile(String path) throws Exception
	{
		String lower = path.toLowerCase();
		if (lower.endsWith(".hex") || lower.endsWith(".json")) {
			loadHex(path);
		} else if (lower.endsWith(".bin") || !path.contains(".")) {
			try {
				loadBin(path);
			} catch (IOException ex) {
				messages("ELF load failed: " + ex.getMessage() + ". Trying raw binary...");
				loadRawBin(path);
			}
		} else if (lower.endsWith(".class")) {
			loadClass(path);
		} else if (lower.endsWith(".jar")) {
			loadJar(path);
		} else if (lower.endsWith(".jll")) {
			loadJll(path);
		} else {
			String ext = path.contains(".") ? path.substring(path.lastIndexOf(".")) : "(no extension)";
			throw new IllegalArgumentException("Unsupported file format: " + ext +
				". Supported: .hex, .json, .bin, .class, .jar, .jll");
		}
	}

	JMenu createMenuFile()
	{
		actionFileLoad = new AbstractAction("Load...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				performFileOpen();
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

		addKey(menu.add(actionFileLoad), KeyEvent.VK_L);
		addKey(menu.add(actionFileExit), KeyEvent.VK_X);

		menu.addSeparator();

		JMenu importMenu = new JMenu("Import");
		addIcon(importMenu, "file.gif");

		AbstractAction importBin = new AbstractAction("ELF Binary...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
					{
						String path = fc.getSelectedFile().getCanonicalPath();
						loadBin(path);
						updatePanel(true);
					}
				} catch (FileNotFoundException ex) {
					showError("File not found", ex);
				} catch (Exception ex) {
					showError("Failed to load ELF binary: " + ex.getMessage(), ex);
				}
			}
		};
		importMenu.add(importBin);

		AbstractAction importClass = new AbstractAction("Java Class...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
					{
						String path = fc.getSelectedFile().getCanonicalPath();
						loadClass(path);
						updatePanel(true);
					}
				} catch (FileNotFoundException ex) {
					showError("File not found", ex);
				} catch (Exception ex) {
					showError("Failed to load Java class: " + ex.getMessage(), ex);
				}
			}
		};
		importMenu.add(importClass);

		AbstractAction importJar = new AbstractAction("JAR...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
					{
						String path = fc.getSelectedFile().getCanonicalPath();
						loadJar(path);
						updatePanel(true);
					}
				} catch (FileNotFoundException ex) {
					showError("File not found", ex);
				} catch (Exception ex) {
					showError("Failed to load JAR: " + ex.getMessage(), ex);
				}
			}
		};
		importMenu.add(importJar);

		AbstractAction importJll = new AbstractAction("JLL...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
					{
						String path = fc.getSelectedFile().getCanonicalPath();
						loadJll(path);
						updatePanel(true);
					}
				} catch (FileNotFoundException ex) {
					showError("File not found", ex);
				} catch (Exception ex) {
					showError("Failed to load JLL: " + ex.getMessage(), ex);
				}
			}
		};
		importMenu.add(importJll);

		menu.add(importMenu);
		menu.addSeparator();

		JMenuItem settings = new JMenuItem("CPU Preferences...");
		settings.addActionListener((ActionEvent e) -> {
			showCpuPreferences();
		});
		menu.add(settings);

		menu.setMnemonic('F');

		return menu;
	}

	private void showCpuPreferences()
	{
		java.util.List<String> cpus = readCpuConfig();
		DefaultListModel<String> model = new DefaultListModel<>();
		for (String cpu : cpus){
			model.addElement(cpu);
		}

		JList<String> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroll = new JScrollPane(list);

		JPanel buttonPanel = new JPanel();
		JButton addBtn = new JButton("Add");
		JButton removeBtn = new JButton("Remove");
		JButton upBtn = new JButton("Up");
		JButton downBtn = new JButton("Down");
		buttonPanel.add(addBtn);
		buttonPanel.add(removeBtn);
		buttonPanel.add(upBtn);
		buttonPanel.add(downBtn);

		addBtn.addActionListener((ActionEvent e) -> {
			String input = JOptionPane.showInputDialog(GUI.this, "Enter CPU class name:", "Add CPU", JOptionPane.PLAIN_MESSAGE);
			if (input != null && !input.trim().isEmpty()){
				model.addElement(input.trim());
			}
		});

		removeBtn.addActionListener((ActionEvent e) -> {
			int idx = list.getSelectedIndex();
			if (idx >= 0){
				model.remove(idx);
			}
		});

		upBtn.addActionListener((ActionEvent e) -> {
			int idx = list.getSelectedIndex();
			if (idx > 0){
				String item = model.remove(idx);
				model.add(idx - 1, item);
				list.setSelectedIndex(idx - 1);
			}
		});

		downBtn.addActionListener((ActionEvent e) -> {
			int idx = list.getSelectedIndex();
			if (idx >= 0 && idx < model.getSize() - 1){
				String item = model.remove(idx);
				model.add(idx + 1, item);
				list.setSelectedIndex(idx + 1);
			}
		});

		JPanel prefsPanel = new JPanel(new BorderLayout());
		prefsPanel.add(listScroll, BorderLayout.CENTER);
		prefsPanel.add(buttonPanel, BorderLayout.SOUTH);

		int result = JOptionPane.showConfirmDialog(GUI.this, prefsPanel, "CPU Preferences", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION){
			java.util.List<String> updated = new java.util.ArrayList<>();
			for (int i = 0; i < model.getSize(); i++){
				updated.add(model.get(i));
			}
			writeCpuConfig(updated);

			menuCpu.removeAll();
			ButtonGroup buttonGroup = new ButtonGroup();
			first = null;
			for (String cpu : updated){
				createMenuCpuLine(menuCpu, buttonGroup, cpu);
			}
			menuCpu.revalidate();
			menuCpu.repaint();
		}
	}

	void performTree(String title, JTree tree)
	{
		class MyRenderer extends DefaultTreeCellRenderer
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

		JPanel content = new JPanel(new BorderLayout());
		content.add(sc, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton copyBtn = new JButton("Copy to Clipboard");
		JButton csvBtn = new JButton("Export CSV");
		buttonPanel.add(copyBtn);
		buttonPanel.add(csvBtn);
		content.add(buttonPanel, BorderLayout.SOUTH);

		copyBtn.addActionListener((ActionEvent e) -> {
			StringBuilder sb = new StringBuilder();
			appendNode(tree, (DefaultMutableTreeNode)tree.getModel().getRoot(), sb, 0);
			StringSelection selection = new StringSelection(sb.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
		});

		csvBtn.addActionListener((ActionEvent e) -> {
			try{
				JFileChooser csvChooser = new JFileChooser();
				csvChooser.setSelectedFile(new File(title.replaceAll(" ", "_") + ".csv"));
				if (csvChooser.showSaveDialog(GUI.this) == JFileChooser.APPROVE_OPTION){
					String csvPath = csvChooser.getSelectedFile().getCanonicalPath();
					try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath))){
						exportTreeToCsv(tree, (DefaultMutableTreeNode)tree.getModel().getRoot(), pw, 0);
					}
					messages("Exported to " + csvPath);
				}
			} catch (HeadlessException | IOException ex) {
				messages(ex);
			}
		});

		JDialog d = new JDialog(this, title, true);
		Point p = getLocation();
		p.x += size.width / 10;
		p.y += size.height / 10;
		d.setLocation(p);
		d.setContentPane(content);
		d.pack();
		d.setVisible(true);
	}

	private void appendNode(JTree tree, DefaultMutableTreeNode node, StringBuilder sb, int depth)
	{
		for (int i = 0; i < depth; i++){
			sb.append("  ");
		}
		sb.append(node.getUserObject());
		sb.append("\n");
		for (int i = 0; i < node.getChildCount(); i++){
			appendNode(tree, (DefaultMutableTreeNode)node.getChildAt(i), sb, depth + 1);
		}
	}

	private void exportTreeToCsv(JTree tree, DefaultMutableTreeNode node, PrintWriter pw, int depth)
	{
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < depth; i++){
			line.append(",");
		}
		line.append(node.getUserObject());
		if (depth > 0){
			String prefix = "";
			for (int i = 0; i < depth; i++){
				prefix += ",";
			}
			pw.println(prefix + node.getUserObject());
		} else {
			pw.println("Level " + depth + "," + node.getUserObject());
		}
		for (int i = 0; i < node.getChildCount(); i++){
			exportTreeToCsv(tree, (DefaultMutableTreeNode)node.getChildAt(i), pw, depth + 1);
		}
	}

	void performInterrupt()
	{
		SortedLong sl = new SortedLong();

		for (int i = 0 ; i < cpu.getInterruptCount() ; i++)
		{
			InterruptStatistic is = cpu.getInterruptAt(i);
			long counter = is.getCounter();
			sl.put(counter, is.toString());
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

		addKey(menu.add(actionToolsProfile), KeyEvent.VK_P);
		menu.add(actionToolsInterrupt);
		addKey(menu.add(actionToolsStatistics), KeyEvent.VK_C);

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
		addIcon(actionDebugErase, "erase.gif");

		actionDebugReset = new AbstractAction("Reset")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				reset();
				updatePanel(false);
			}
		};
		addIcon(actionDebugReset, "reset.gif");

		actionDebugStop = new AbstractAction("Stop")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if (cpu != null){
						cpu.stopSimulation();
					}
					if (thread != null){
						thread.interrupt();
					}
				} catch (Exception ex) {
					Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
				}

			}
		};

		addIcon(actionDebugStop, "stop.gif");

		actionDebugTrace = new AbstractAction("Step into")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (cpu == null) {
					showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
					return;
				}
				try
				{
					cpu.step();
					updatePanel(false);
				} catch (Exception ex) {
					showError("Failed to execute step: " + ex.getMessage(), ex);
				}
			}
		};
		addIcon(actionDebugTrace, "stepinto.gif");

		actionDebugGo = new AbstractAction("Go")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (cpu == null) {
					showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
					return;
				}
				thread = new Thread(() -> {
					messages("Simulating ....");
					try
					{
						cpu.go(-1);
					} catch (InterruptedException ex) {
						// Normal stop, not an error
					} catch (Exception ex) {
						GUI.this.showError("Simulation failed: " + ex.getMessage(), ex);
					}


					SwingUtilities.invokeLater(() -> {
						emulation(false);
					});
				});

				emulation(true);

				thread.start();
			}


		};
		addIcon(actionDebugGo, "play.gif");

		actionDebugStep = new AbstractAction("Step over")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (cpu == null) {
					showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
					return;
				}
				thread = new Thread(() -> {
					messages("Emulating ....");
					try
					{
						cpu.pass();
					} catch (InterruptedException ex) {
						// Normal stop, not an error
					} catch (Exception ex) {
						GUI.this.showError("Step over failed: " + ex.getMessage(), ex);
					}

					SwingUtilities.invokeLater(() -> {
						emulation(false);
					});
				});

				emulation(true);

				thread.start();
			}


		};
		addIcon(actionDebugStep, "step.gif");

		JMenu menu = new JMenu("Debug");
		addIcon(menu, "debug.gif");

		addKey(menu.add(actionDebugTrace), KeyEvent.VK_I);
		addKey(menu.add(actionDebugStep), KeyEvent.VK_O);
		addKey(menu.add(actionDebugReset), KeyEvent.VK_R);
		addKey(menu.add(actionDebugGo), KeyEvent.VK_G);
		addKey(menu.add(actionDebugStop), KeyEvent.VK_S);
		addKey(menu.add(actionDebugErase), KeyEvent.VK_E);

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
			Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
			System.exit(1);
		}
	}

	public void updatePanel(boolean force)
	{
		if (cpu == null){
			return;
		}

		for (int i = 0 ; i < panels.size() ; i++){
			panels.get(i).update(force);
		}
	}
}

package AI;

import jx.devices.pci.PCIGod;
import static jx.init.Main.main;
import jx.netmanager.NetInit;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Naming;
import jx.zero.debug.DebugChannel;
import org.jnode.fs.fat.FatFileSystem;
import test.fs.BioRAMDomain;
import test.fs.FSDomain;
import timerpc.StartTimer;

/**
 * This is a class initialize an artificial intelligence service.
 * 
 * @author X. Wang
 * @version 1.0
 */
public final class AI 
{
    // instance variables
    private final AIMemory mem = new AIMemory();
    private final AIInput  inp;
    private final AILogic  log;
    private final AIOutput oup;
    private final Thread   logThread, inpThread, oupThread;
    
    /**
     * Constructor for objects of class AI
     */
    public AI()
    {
        // Initialize instance variables
        mem.setLogPath("/AI/");
        inp = new AIInput(mem);
        log = new AILogic(mem);
        oup = new AIOutput(mem);
	logThread = new Thread(log);
        inpThread = new Thread(inp);
        oupThread = new Thread(oup);
    }
    
    public void start()
    {
        Debug.out.println("AI running...");
    	logThread.start();
        inpThread.start(); 
        oupThread.start();
    }
    
    public static void init(Naming naming) throws Exception {
	jx.zero.debug.DebugOutputStream out = new jx.zero.debug.DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
	Debug.out = new jx.zero.debug.DebugPrintStream(out);
	//System.out = new java.io.PrintStream(out);
	//System.err = System.out;
	 
	Debug.out.println("Init running...");
        PCIGod.main(new String[]{});
        StartTimer.main(new String[]{"TimerManager"});
        main(new String[] {"boot.rc"});
        bioide.Main.main(new String[]{"TimerManager", "BIOFS_RW", "0", "0"});
        BioRAMDomain.main(new String[]{"BIOFS"});
        NetInit.init(InitialNaming.getInitialNaming(), new String[]{"NIC", "eth0", "8:0:6:28:63:40"});
	//FSDomain.main(new String[]{"BIOFS", "FS"});
        AI instance = new AI();
        instance.start();
     }
}
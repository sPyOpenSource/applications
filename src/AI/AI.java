package AI;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import jx.console.ConsoleImpl;
import jx.devices.pci.PCIGod;
import static jx.init.Main.main;
import jx.netmanager.NetInit;
import jx.zero.Clock;
import jx.zero.CycleTime;
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
        mem.ImportTxt("");
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
        bioide.Main.main(new String[]{"TimerManager", "BIOFS_RW", "1", "1"});
        
        NetInit.init(InitialNaming.getInitialNaming(), new String[]{"NET"});
        AI instance = new AI();
	FSDomain.main(new String[]{"BIOFS_RW", "FS"});
        
        //instance.start();
        //jx.keyboard.Main.main(new String[]{"WindowManager"});
        /*try (
            DatagramSocket clientSocket = new DatagramSocket(9000)) {
            InetAddress IPAddress = InetAddress.getByName("home");
            byte[] receiveData = new byte[1024];
            String sentence = "helloworld";
            Debug.out.println(sentence);
            byte[] sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            //clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String modifiedSentence = new String(receivePacket.getData());
            Debug.out.println("FROM SERVER:" + modifiedSentence);
        }*/
        //ConsoleImpl.init(InitialNaming.getInitialNaming());
        test.net.WebServer.main(new String[]{"-fs", "FS", "-threads"});
    }
}
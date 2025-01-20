package AI;

import jx.console.ConsoleImpl;
import jx.emulation.Init;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Naming;
import jx.zero.debug.DebugChannel;
import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugPrintStream;

/**
 * This is a class initialize an artificial intelligence service.
 * 
 * @author X. Wang
 * @version 1.0
 */
public final class AI
{
    // instance variables
    private final AIIO IO = new AIIO();
    private final AILogic log;
    private final Thread logThread;
    
    /**
     * Constructor for objects of class AI
     */
    public AI()
    {
        // Initialize instance variables
        log = new AILogic(IO.getMemory());
        logThread = new Thread(log, "logic");
    }
    
    public void start()
    {
        System.out.println("AI running...");
        logThread.start();
        IO.start();
    }
    
    public static void init(Naming naming) throws Exception {
        DebugOutputStream out = new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
        Debug.out = new DebugPrintStream(out);
        //System.setOut(new java.io.PrintStream(out));
        //System.setErr(System.out);

        Debug.out.println("Init running...");
        main(null);
    }
    
    public static void main(String[] args){
        try {
            //jx.init.Main.main(new String[] {"boot.rc"});
            //POST post = new POST(Init.naming);
            //post.test();
            //test.net.WebServer.main(new String[]{"-fs", "FS", "-threads"});
            /*if(InitialNaming.getInitialNaming() == null){
                Init.init();
                InitialNaming.naming = Init.naming;
            }*/
            AI instance = new AI();
            instance.start();
            //jx.keyboard.Main.main(new String[]{"WindowManager"});
            ConsoleImpl.init(InitialNaming.getInitialNaming());
        } catch (Exception ex) {
            //Logger.getLogger(AI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

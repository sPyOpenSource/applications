package AI;

import java.util.logging.Level;
import java.util.logging.Logger;
import jx.emulation.Init;

import jx.zero.Debug;
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
    private final AIMemory hci  = new AIMemory();
    private final AILogic logic = new AILogic(hci);
    
    public void start()
    {
        Debug.out.println("AI running...");
        //hci.start();
        logic.start();
    }
    
    public static void init(Naming naming) throws Exception {
        DebugOutputStream out = new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0"));
        Debug.out = new DebugPrintStream(out);
        //System.setOut(new java.io.PrintStream(out));
        //System.err = System.out;
        
        Debug.out.println("Init running...");
        main(null);
    }
    
    public static void main(String[] args){
        try {
            jx.init.Main.main(new String[] {"boot.rc"});
            POST post = new POST(Init.naming);
            post.test();
            //test.net.WebServer.main(new String[]{"-fs", "FS", "-threads"});
            AI instance = new AI();
            instance.start();
            //jx.keyboard.Main.main(new String[]{"WindowManager"});
            //ConsoleImpl.init(Init.naming);
        } catch (Exception ex) {
            Logger.getLogger(AI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

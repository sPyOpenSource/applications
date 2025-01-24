
package AI;

import jx.zero.Naming;
import jx.zero.Ports;
import jx.zero.debug.DebugOutputStream;

/**
 *
 * @author X. Wang
 */
public class AIIO {
    private final AIMemory mem;
    private final AIInput  inp;
    private final AIOutput out;
    private final Thread   inpThread, outThread;
    private Ports ports; // You can access any address with ports in the computer memory

    public AIIO(Naming naming){
        ports = (Ports)naming.lookup("Ports");
        mem = new AIMemory(naming);
        mem.setLogPath("/AI/");
        inp = new AIInput(mem);
        out = new AIOutput(mem);
        inpThread = new Thread(inp, "input");
        outThread = new Thread(out, "output");
    }
    
    public void start()
    {
        inpThread.start();
        outThread.start();
        mem.ImportBackup("/ai");
    }
    
    public AIMemory getMemory(){
        return mem;
    }
    
    public DebugOutputStream getOut(){
        return out.getOut();
    }
}

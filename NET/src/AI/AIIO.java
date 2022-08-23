
package AI;

/**
 *
 * @author X. Wang
 */
public class AIIO {
    private final AIMemory mem = new AIMemory();
    private final AIInput  inp;
    private final AIOutput out;
    private final Thread   inpThread, outThread;

    public AIIO(){
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
}

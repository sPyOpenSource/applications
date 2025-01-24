
package AI;

import jx.devices.pci.PCIAccess;
import jx.devices.pci.PCIGod;
import jx.zero.Debug;
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
    private final Ports ports; // You can access any address with ports in the computer memory
    private final PCIAccess depHandle;

    public AIIO(Naming naming){
        ports = (Ports)naming.lookup("Ports");
        PCIGod god = new PCIGod(naming);
        // promote as DEP
        depHandle = god;
        mem = new AIMemory(naming);
        // register as DEP
        mem.getInitialNaming().registerPortal(depHandle, "PCIAccess");
        mem.setLogPath("/AI/");
        inp = new AIInput(mem);
        out = new AIOutput(mem);
        Debug.out.println("PCIAccess registered");
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

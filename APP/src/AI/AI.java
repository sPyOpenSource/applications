package AI;

import jx.devices.pci.PCIAccess;
import jx.devices.pci.PCICodes;
import jx.devices.pci.PCIDevice;
import jx.netmanager.NetInit;

import jx.zero.Naming;
import org.jnode.driver.bus.usb.uhci.UHCIDriver;

/**
 * This is a class initialize an artificial intelligence service.
 * 
 * @author X. Wang
 * @version 1.0
 */
public final class AI
{
    // instance variables
    private final AIIO IO;
    private final AILogic log;
    private final Thread logThread;
    
    /**
     * Constructor for objects of class AI
     */
    public AI(Naming naming)
    {
        IO = new AIIO(naming);
        log = new AILogic(IO.getMemory());
        //NetInit.init(IO.getMemory().getInitialNaming(), new String[]{"NET"});
        PCIAccess pci = (PCIAccess)IO.getMemory().getInitialNaming().lookup("PCIAccess");
        for(int i = 0; i < pci.getNumberOfDevices(); i++){
            PCIDevice dev = pci.getDeviceAt(i);
            if(PCICodes.lookupClass(dev.getClassCode()).startsWith("USB")){
                System.out.println("USB found");
                new UHCIDriver(dev);
            }
        }
        logThread = new Thread(log, "logic");
    }
    
    public void start()
    {
        logThread.start();
        IO.start();
        System.out.println("AI running...");
    }
    
    public AIIO getIO(){
        return IO;
    }
    
    public static void init(Naming naming) throws Exception {
        AI instance = new AI(naming);
        instance.start();
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
            //AI instance = new AI();
            //instance.start();
            //jx.keyboard.Main.main(new String[]{"WindowManager"});
            //ConsoleImpl.init(InitialNaming.getInitialNaming());
        } catch (Exception ex) {
            //Logger.getLogger(AI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

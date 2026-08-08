package AI;

import jx.devices.pci.PCIAccess;
import jx.devices.pci.PCICodes;
import jx.devices.pci.PCIDevice;
import jx.netmanager.NetInit;
import jx.zero.Debug;
import jx.zero.InitialNaming;

import jx.zero.Naming;
import jx.zero.debug.DebugChannel;
import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugPrintStream;

import org.jnode.driver.block.usb.storage.scsi.USBStorageMount;
import org.jnode.driver.bus.usb.USBHubMonitor;
import org.jnode.driver.bus.usb.uhci.UHCIDriver;

import test.fs.MediaChangeTest;
import test.fs.LargeFileTest;
import test.fs.USBMassStorageTest;

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
    private final USBHubMonitor[] monitors = new USBHubMonitor[8];
    
    /**
     * Constructor for objects of class AI
     * @param naming
     */
    public AI(Naming naming)
    {
        Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) InitialNaming.getInitialNaming().lookup("DebugChannel0")));
        IO = new AIIO(naming);
        log = new AILogic(IO.getMemory());
        //NetInit.init(IO.getMemory().getInitialNaming(), new String[]{"NET"});
int j = 0;
        PCIAccess pci = (PCIAccess)IO.getMemory().getInitialNaming().lookup("PCIAccess");
        for(int i = 0; i < pci.getNumberOfDevices(); i++){
            PCIDevice dev = pci.getDeviceAt(i);
            if(PCICodes.lookupClass(dev.getClassCode()).startsWith("USB")){
                UHCIDriver driver = new UHCIDriver(dev, log.getSM());
                USBHubMonitor monitor = new USBHubMonitor(dev, driver.getAPI(), log.getSM());
                // Wire USB mass storage devices: on attach, mount a FatFileSystem
                monitor.addAttachListener(new USBStorageMount());
                monitors[j++] = monitor;
            }
        }

        logThread = new Thread(log, "logic");
        while(true){
            for (USBHubMonitor monitor : monitors) {
                if (monitor == null) {
                    continue;
                }
                //System.out.println(i);
                monitor.startMonitor();
            }
            break;
        }
    }
    
    public void start()
    {
        logThread.start();
        IO.start();
        // Verify the USB mass storage mount (waits for the USBFS portal)
        new Thread(new Runnable() {
            @Override
            public void run() {
                USBMassStorageTest.main(null);
            }
        }, "USBMassStorageTest").start();
        // Media change / eject/insert test
        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaChangeTest.main(null);
            }
        }, "MediaChangeTest").start();
        // Large file / multi-sector transfer test
        new Thread(new Runnable() {
            @Override
            public void run() {
                LargeFileTest.main(null);
            }
        }, "LargeFileTest").start();
        System.out.println("AI running...");
    }
    
    public AIIO getIO(){
        return IO;
    }
    
    public static void init(Naming naming) throws Exception {
        AI instance = new AI(naming);
        instance.start();
        //jx.init.Main.main(new String[] {"boot.rc"});
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

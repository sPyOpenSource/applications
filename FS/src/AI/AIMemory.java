package AI;

import static AI.AIZeroLogic.getHash;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jx.InitialNaming;
import jx.devices.bio.BlockIO;
import jx.devices.pci.PCIGod;

import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Ports;

/**
 * This is the memory class of AI.
 * 
 * @author X. Wang
 * @version 1.0
 */
public class AIMemory extends AIZeroMemory
{
    // instance variables
    //private SerialPort serialPort;
    private final AIInput  inp;
    private final AIOutput out;
    private final Thread   inpThread, outThread;
    private BlockIO drive;
    private Memory buffer;
    private MemoryManager mManager;
    private Ports ports; // You can access any address with ports in the computer memory
    private TreeMap<String, TreeMap> tree = new TreeMap<>();
    
    private final int length = 101;

    /**
     * Constructor for objects of class AIMemory
     */
    public AIMemory()
    {
        mManager = (MemoryManager)InitialNaming.lookup("MemoryManager");
        ports = (Ports)InitialNaming.lookup("Ports");
        buffer =  mManager.alloc(512);
        inp = new AIInput(this);
        out = new AIOutput(this);
        inpThread = new Thread(inp, "input");
        outThread = new Thread(out, "output");
        
        try{
            PCIGod.main(new String[]{});

            //bioide.Main.main(new String[]{"TimerManager", "BioRAM", "full", "0"});

            //NetInit.init(InitialNaming.getInitialNaming(), new String[]{"NET"});

            //FSDomain.main(new String[]{"BioRAM", "FS"});
            
            // Initialize instance variables
            /*try {
                serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier("/dev/ttyACM0").open(this.getClass().getName(), 2000);
                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE); 
            } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException ex) {
                Logger.getLogger(AIMemory.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            
            //drive = (BlockIO)LookupHelper.waitUntilPortalAvailable(null, "BioRAM");
        } catch (ExceptionInInitializerError | NullPointerException ex){
            Logger.getLogger(AIMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*public SerialPort getSerialPort(){
        return serialPort;
    }*/

    public String read(String name) {
        TreeMap<String, TreeMap> current = tree;
        for(String part:name.split("/")){
            current = current.get(part);
        }
        if(current != null){
        Memory bufferRead =  mManager.alloc(512);
            drive.readSectors(getHash(name, length), 1, bufferRead, true);
            for(int i = 0; i < 512; i++){
                Debug.out.print(bufferRead.get8(i));
            }
        }
        return null;
    }
    
    public void write(String name){
        TreeMap<String, TreeMap> current = tree;
        for( String part:name.split("/")){
            if(current.containsKey(part)){
                current = current.get(part);
            } else {
                TreeMap<String, TreeMap> temp = new TreeMap<>();
                current.put(part, temp);
                current = temp;
            }
        }
        drive.writeSectors(getHash(name, length), 1, buffer, true);
    }
    
    @Override
    public void ImportBackup(String file){
        buffer.set8(0, (byte)60);
        write("ai.txt");
        read("ai.txt");
    }
    
    public void start()
    {
        inpThread.start();
        outThread.start();
        ImportBackup("/ai");
    }
}

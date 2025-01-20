package AI;

import java.util.TreeMap;

import jx.zero.InitialNaming;
import jx.devices.bio.BlockIO;
import jx.devices.pci.PCIGod;
import jx.fs.buffer.BufferCache;
import jx.fs.FileSystem;
import jx.fs.Node;

import jx.zero.Clock;
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
public class AIMemory extends AIZeroMemory implements FileSystem
{
    // instance variables
    //private SerialPort serialPort;
    private BlockIO drive;
    private final int length = 101;
    private Memory buffer;
    private Ports ports; // You can access any address with ports in the computer memory
    private TreeMap<String, TreeMap> tree = new TreeMap<>();
    
    /**
     * Constructor for objects of class AIMemory
     */
    public AIMemory()
    {
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
            MemoryManager memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
            ports = (Ports)InitialNaming.getInitialNaming().lookup("Ports");
            buffer =  memoryManager.alloc(512);
        } catch (ExceptionInInitializerError | NullPointerException ex){
            //Logger.getLogger(AIMemory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*public SerialPort getSerialPort(){
        return serialPort;
    }*/

    @Override
    public void init(BlockIO blockDevice, BufferCache bufferCache, Clock clock) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getRootNode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init(boolean read_only) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void release() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void build(String name, int blocksize) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void check() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getNode(int identifier) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getDeviceID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public int getHash(String name){
        int value = 0;
        for (int i = 0; i < name.length(); i++ )
            value += name.charAt(i);
        return ( value * name.length() ) % length + 100;
    }

    public String read(String name) {
        TreeMap<String, TreeMap> current = tree;
        for(String part:name.split("/")){
            current = current.get(part);
        }
        if(current != null){
        MemoryManager memoryManager = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
        Memory buf =  memoryManager.alloc(512);
            drive.readSectors(getHash(name), 1, buf, true);
            for(int i = 0; i < 512; i++){
                Debug.out.print(buf.get8(i));
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
        drive.writeSectors(getHash(name), 1, buffer, true);
    }
    
    @Override
    public void ImportBackup(String file){
        buffer.set8(0, (byte)60);
        write("ai.txt");
        read("ai.txt");
    }
}
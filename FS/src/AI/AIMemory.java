package AI;

import java.util.TreeMap;
import jx.devices.bio.BlockIO;
import jx.fs.buffer.BufferCache;
import jx.fs.FileSystem;
import jx.fs.Node;

import jx.zero.Clock;
import jx.zero.Debug;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Naming;

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
    private final TreeMap<String, TreeMap> root = new TreeMap<>();
    private final int length = 101;
    private final Naming naming;
    private BlockIO drive;
    private Memory buffer;
    private MemoryManager memoryManager;
    
    /**
     * Constructor for objects of class AIMemory
     */
    public AIMemory(Naming naming)
    {
        this.naming = new jx.InitialNaming(naming);
        try{
            bioide.Main.main(new String[]{"TimerManager", "BioRAM", "0", "0"});

            //FSDomain.main(new String[]{"BioRAM", "FS"});
            // Initialize instance variables
            /*try {
                serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier("/dev/ttyACM0").open(this.getClass().getName(), 2000);
                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE); 
            } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException ex) {
                Logger.getLogger(AIMemory.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            //drive = (BlockIO)LookupHelper.waitUntilPortalAvailable(null, "BioRAM");
            memoryManager = (MemoryManager)naming.lookup("MemoryManager");
            buffer = memoryManager.alloc(512);
            /*for(int i = 0; i < buffer.size(); i++){
                buffer.set8(i, (byte)0xff);
            }*/
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

    public Memory read(String name) {
        TreeMap<String, TreeMap> current = root;
        for(String part:name.split("/")){
            current = current.get(part);
        }
        if(current != null){
            drive.readSectors(AIZeroLogic.getHash(name, length), 1, buffer, true);
            for(int i = 0; i < 512; i++){
                Debug.out.print(buffer.get8(i));
            }
        }
        return buffer;
    }
    
    public void write(String name){
        TreeMap<String, TreeMap> current = root;
        for( String part:name.split("/")){
            if(current.containsKey(part)){
                current = current.get(part);
            } else {
                TreeMap<String, TreeMap> temp = new TreeMap<>();
                current.put(part, temp);
                current = temp;
            }
        }
        drive.writeSectors(AIZeroLogic.getHash(name, length), 1, buffer, true);
    }
    
    @Override
    public void ImportBackup(String file){
        //write("ai.txt");
        //read("ai.txt");
    }

    Naming getInitialNaming() {
        return naming;
    }
}
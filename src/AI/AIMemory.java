package AI;

import jx.InitNaming;
import jx.bio.BlockIO;
import jx.fs.FSException;
import jx.fs.Inode;
import jx.fs.buffercache.BufferCache;
import jx.zero.Clock;
import jx.zero.Debug;
import jx.zero.LookupHelper;
import jx.zero.Memory;
import jx.zero.MemoryManager;

/**
 * This is the memory class of AI.
 * 
 * @author X. Wang 
 * @version 1.0
 */
public class AIMemory extends AIZeroMemory implements jx.fs.FileSystem
{
    // instance variables
    //private SerialPort serialPort;
    private final BlockIO drive;
    private final int length = 101;
    private Memory buffer;
    
    /**
     * Constructor for objects of class AIMemory
     */
    public AIMemory()
    {
        // Initialize instance variables
        /*try {
            serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier("/dev/ttyACM0").open(this.getClass().getName(), 2000);
            serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE); 
        } catch (NoSuchPortException | PortInUseException | UnsupportedCommOperationException ex) {
            Logger.getLogger(AIMemory.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        drive = (BlockIO)LookupHelper.waitUntilPortalAvailable(null, "BIOFS_RW");
        MemoryManager memoryManager = (MemoryManager)InitNaming.lookup("MemoryManager");
        buffer =  memoryManager.alloc(512);
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
    public Inode getRootInode() {
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
    public Inode getInode(int identifier) throws FSException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer getDeviceID() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public int getHash(String name){
        int value = 0;
    for (int i = 0; i < name.length(); i++ )
        value += name.charAt(i);
    return ( value * name.length() ) % length + 100;
    }

    @Override
    public String read(String name) {
        MemoryManager memoryManager = (MemoryManager)InitNaming.lookup("MemoryManager");
        Memory buffer2 =  memoryManager.alloc(512);
            drive.readSectors(getHash(name), 1, buffer2, true);
            for(int i = 0; i < 512; i++){
                Debug.out.print(buffer2.get8(i));
            }
            return null;
    }
    
    public void write(String name){
        drive.writeSectors(getHash(name), 1, buffer, true);
    }
    
    @Override
    public void ImportTxt(String file){
        buffer.set8(0, (byte)60);
        write("ai.txt");
        read("ai.txt");
    }
}
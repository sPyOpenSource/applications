package test.fs;


import jx.fs.*;
import jx.zero.*;

/**
 * Create and remove files
 *
 * @author Michael Golm
 */

public class CreateRemove {
    private static final int MAXBUFFERSIZE   = 1*1024*1024;

    public static void main(String[] args) throws Exception {
    Naming naming = InitialNaming.getInitialNaming();
    MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");
    Memory buffer = memMgr.allocAligned(MAXBUFFERSIZE, 4);
    
    FS fs = null ;
    while((fs = (FS)naming.lookup("FS")) == null) Thread.yield();

    fs.create("TEST0", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
    fs.create("TEST1", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);
    
    jx.fs.FileSystem jfs = (jx.fs.FileSystem)naming.lookup("JavaFS");    
    if (jfs != null)
        jfs.release();

    Debug.out.println("Test finished.");
    }
    

    static void dotest(jx.fs.FileSystem fs) throws FSException {
    Node ino =  fs.getRootNode();
    ino.create("bla1", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
    ino.create("bla0", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
    }
}

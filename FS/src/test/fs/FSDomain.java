package test.fs;

import AI.AIMemory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jx.devices.bio.BlockIO;
import vfs.FSImpl;

import jx.fs.InodeImpl;
import jx.zero.CPUManager;
import jx.zero.Clock;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.LookupHelper;
import jx.zero.Naming;
import jx.zero.debug.DebugChannel;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;
import org.jnode.fs.jfat.FatFileSystem;
import org.jnode.fs.jfat.FatRootDirectory;

public class FSDomain {
    Naming naming;
    static final int EXT2FS_BLOCKSIZE = 1024;

    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
	//cpuManager.setThreadName("FSDomain-Main");
	BlockIO bio = (BlockIO)LookupHelper.waitUntilPortalAvailable(naming, args[0]);
	if (args.length > 2 && args[2].equals("-format")) {
            new FSDomain(naming, bio, args[1], true);
	} else if (args.length > 2 && args[2].equals("-noformat")) {
	    new FSDomain(naming, bio, args[1], false);
	} else {
            new FSDomain(naming, bio, args[1], false);
	}
    }

    public static void init(Naming naming, String [] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	main(args);
    }
    
    FSDomain(final Naming naming, BlockIO bio, String fsname, boolean format) {
	//try {
	    this.naming = naming;
            
	    final FSImpl fs = new FSImpl();
            final jx.fs.FileSystem fat = new FatFileSystem(bio);
            FatRootDirectory root = (FatRootDirectory)fat.getRootNode();
        /*try {
            root.getFatDirEntry(0, false);
        } catch (IOException ex) {
            //Logger.getLogger(FSDomain.class.getName()).log(Level.SEVERE, null, ex);
        }*/
            //final jx.fs.FileSystem fat = new AIMemory(naming);
	    //final javafs.FileSystem jfs = new javafs.FileSystem();
	    Clock clock = new DummyClock();
	    //jfs.init(bio, new buffercache.BufferCache(bio, clock, 800, 1000, 100, EXT2FS_BLOCKSIZE), clock);
	    Debug.out.println("Capacity: " + bio.getCapacity());

	    if (format) {
		//Profiler profiler = (Profiler)naming.lookup("Profiler");
		//profiler.startSampling();
		//jfs.build("TestFS", 1024);
		//profiler.stopSampling();
	    }
	    fs.mountRoot(fat, false /* read-only = false*/);

	    if (format) {
		//fs.mkdir("lost+found", InodeImpl.S_IWUSR | InodeImpl.S_IRUGO | InodeImpl.S_IXUGO);
	    }

	    naming.registerPortal(fs, fsname);
	    naming.registerPortal(fat, "JavaFS");
	/*} catch(Exception e) {
	    Debug.out.println("EXCEPTION: " + e);
	    throw new Error();
	}*/
    }
}

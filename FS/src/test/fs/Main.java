package test.fs;

import jx.bio.ram.BlockIORAM;
import jx.devices.bio.BlockIO;
import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

import jx.fs.FS;
import jx.fs.FSException;
import jx.fs.FileSystem;
import jx.fs.InodeImpl;
import jx.fs.Node;

class IOZoneRAMSingle {
    public static void init(Naming naming) throws Exception {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	Main.useRAM = true;
	Main.singleDomain = true;
	new Main(naming);
    }
}

interface FinishNotify extends Portal {
    void finished();
}

public class Main {
    static boolean useRAM = false;
    static boolean singleDomain = false;
    public static int IOZONE_MAX_FILESIZE = 2 * 4096; /* in kBytes */
    public static int IOZONE_MIN_FILESIZE = 4;
    public static int IOZONE_MIN_RECSIZE = 4 * 1024;
    public static int IOZONE_MAX_RECSIZE = 16 * 1024 * 1024;
    final int BUFFERCACHE_NUMBER_FSBLOCKS = 1 * 1024; /*  kBytes */
    final int BUFFERCACHE_MAXNUMBER_FSBLOCKS = 1 * 1024; /* kBytes */
    final int BUFFERCACHE_INCNUMBER_FSBLOCKS = 0; /* 0 MBytes, do not enlarge buffer */

    static Naming naming;
    static DomainManager dm;
    static final int EXT2FS_BLOCKSIZE = 1024;
    jx.bio.buffercache.BufferCache bufferCache;

    public void dotest(FS fs) throws FSException {
	// choose test
	//fsckTest();
	//fileTreeWalkTest();
	iozoneTest(fs);
    }

    public Main(Naming naming) throws Exception {
	this(naming, "FS");
    }

    public Main(Naming naming, String fsname) throws Exception {
	jx.fs.FileSystem jfs = null;
	dm = (DomainManager)naming.lookup("DomainManager");
        if (singleDomain) {
            throw new Error();
        } else {
            FS fs = (FS)LookupHelper.waitUntilPortalAvailable(naming, fsname);

            dotest(fs);

            jfs = (jx.fs.FileSystem)naming.lookup("JavaFS");
        }
        if (jfs != null)
            jfs.release();
    }

    public boolean iozoneTest(FS fs) throws FSException {
	Debug.out.println("starting IOZONE");
	IOZONE iozone = new IOZONE(fs, IOZONE_MIN_FILESIZE, IOZONE_MAX_FILESIZE, IOZONE_MIN_RECSIZE, IOZONE_MAX_RECSIZE);
	return true;
    }


    public boolean fsckTest() throws FSException {
        FS fs = (FS) naming.lookup("FS");

        BlockIO bio = (BlockIO)naming.lookup("IDE");

        FileSystem jfs = new jx.fs.javafs.FileSystem();
        Clock clock = new DummyClock();
        jfs.init(bio, new jx.bio.buffercache.BufferCache(bio, clock, 500, 1000, 100, EXT2FS_BLOCKSIZE), clock);

        naming.registerPortal(jfs, "JavaFS");


        // TEST
        Debug.out.println("Kapazitaet: " + bio.getCapacity());

        FileSystem filesystem = (FileSystem)jfs;

        filesystem.build("TestFS", 1024);


        fs.mountRoot(filesystem, false); // 2. Parameter = read-only  //hda8

        fs.mkdir("lost+found", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);


        fs.mkdir("d1", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
        fs.mkdir("d2", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);
        fs.mkdir("d3", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO|InodeImpl.S_IXUGO);

        fs.create("iozone.tmp", InodeImpl.S_IWUSR|InodeImpl.S_IRUGO);

        InodeImpl inode = (InodeImpl)fs.lookup("iozone.tmp");
        MemoryManager memMgr = (MemoryManager)InitialNaming.getInitialNaming().lookup("MemoryManager");

        Memory buffer = memMgr.alloc(368 * 1024 ); // 100 blocks are 2-indirect


        for(int i = 0; i < buffer.size(); i++) {
            buffer.set8(i, (byte)(i & 0xff));
        }

        int reclen = 4096;
        int numrecs = buffer.size() / reclen;
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < numrecs; i++) {
                inode.write(buffer, i*reclen, reclen);
            }
        }

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < numrecs; i++) {
                inode.read(buffer, i*reclen, reclen);
            }
        }


        //inode.write(buffer, 0, buffer.size());

        inode.decUseCount();

        jfs.printStatistics();
        jfs.release();
        jfs.printStatistics();

        Debug.out.println("*** CHECKING FILESYSTEM ***");
        jfs.check();

        return true;
    }

    public boolean rereadTest(FS fs) throws FSException {
	    new ReRead(fs);
	    return true;
    }


    void fileTreeWalkTest() throws FSException {
	FS fs = (FS) naming.lookup("FS");
	BootFS bootFS = (BootFS) naming.lookup("BootFS");
	Memory file = bootFS.getReadWriteFile("diskImage.ext2");
	Debug.out.println("DISKIMAGE:");
	Dump.xdump1(file, 0, 256);


	BlockIORAM bio = new BlockIORAM(file);
	FileSystem jfs = new jx.fs.javafs.FileSystem();
	Clock clock = new DummyClock();
	jfs.init(bio, new jx.bio.buffercache.BufferCache(bio, clock, 500, 1000, 100, EXT2FS_BLOCKSIZE), clock);
	jfs.init(false);

	// TEST
	Debug.out.println("Kapazitaet: " + bio.getCapacity());

	FileSystem filesystem = (FileSystem)jfs;

	//filesystem.build("TestFS", 1024);
	fs.mountRoot(filesystem, false); // 2. Parameter = read-only  //hda8
	
	printDir(" ", fs.getCwdNode());

    }
    private void printDir(String space, Node dirInode) throws FSException {
	Node inode;
	String[] names = dirInode.readdirNames();
        for (String name : names) {
            inode = dirInode.lookup(name);
            Debug.out.print(space);
            if (inode.isDirectory())
                Debug.out.print(" D");
            else if (inode.isFile())
                Debug.out.print(" F");
            else if (inode.isSymlink())
                Debug.out.print(" L");
            Debug.out.print(" " + inode.getLength());
            inode.decUseCount();
            Debug.out.print("  \t\t" + name);
            if (inode.isSymlink())
                Debug.out.print(" -> " + inode.getSymlink());
            Debug.out.println();
        }
        for (String name : names) {
            inode = dirInode.lookup(name);
            if (! name.equals(".") && ! name.equals("..") && inode.isDirectory()) {
                Debug.out.println(space + "-------------");
                Debug.out.println(space + name + ":");
                printDir(space + "  ", inode);
            }
            inode.decUseCount();
        }
    }
}

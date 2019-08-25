package bioide;

import jx.InitNaming;
import jx.zero.*;
import jx.bio.*;
import jx.fs.Inode;
import jx.fs.InodeIOException;
import jx.fs.InodeNotFoundException;
import jx.fs.NoDirectoryInodeException;
import jx.fs.NoFileInodeException;
import jx.fs.NotExistException;
import jx.fs.PermissionException;
import org.jnode.fs.fat.FatFileSystem;
import vfs.FSImpl;

public class Main {
    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	String bioName = args[1];
	int drive = Integer.parseInt(args[2]);

	IDEDeviceImpl ide = new IDEDeviceImpl();
	Debug.out.println("*1");
	Drive[] drives = ide.getDrives();
	BlockIO bio;

	if (args[3].equals("full")) {
	    bio = drives[drive];
	} else {
	    int partition = Integer.parseInt(args[3]);
	    Debug.out.println("*2");
	    PartitionEntry[] partitions = drives[drive].getPartitions();
	    Debug.out.println("*3");
	    
	    bio = partitions[partition];
	}
	naming.registerPortal(bio, bioName);
        FatFileSystem fat = new FatFileSystem(bio);
        final FSImpl fs = new FSImpl();
        fs.mountRoot(fat, false /* read-only = false*/);
        InitNaming.registerPortal(fs, "FS");
        try {
            Inode inode = fs.lookup("/INDEX~1.HTM");
            int l = inode.getLength();
            Memory bufferx = Env.memoryManager.allocAligned(512, 8);
            try {
                inode.read(bufferx, 0, l);
                for(int i = 0; i < l; i++)
                    Debug.out.println(bufferx.get8(i));
            } catch (NoFileInodeException ex) {
                //Logger.getLogger(PartitionTable.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InodeIOException | InodeNotFoundException | NoDirectoryInodeException | NotExistException | PermissionException ex) {
            //Logger.getLogger(PartitionTable.class.getName()).log(Level.SEVERE, null, ex);
        }
	Debug.out.println("Block I/O device registered as " + bioName);	
    }
}

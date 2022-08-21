package test.fs;

import jx.zero.*;
import jx.bio.BlockIO;
import bioide.IDEDeviceImpl;
import bioide.Drive;
import bioide.PartitionEntry;

public class IDEDomain {
    
    public static void main(String [] args) {
    Naming naming = InitialNaming.getInitialNaming();
    String bioName = args[1];
    int drive = Integer.parseInt(args[2]);

    IDEDeviceImpl ide = new IDEDeviceImpl();
    Debug.out.println("*1");
    Drive[] drives = ide.getDrives();
    BlockIO bio = null;

    if (args[3].equals("full")) {
        bio = drives[drive];
    } else {
        int partition = Integer.parseInt(args[3]);
        Debug.out.println("*2");
        PartitionEntry[] partitions = drives[drive].getPartitions();
        Debug.out.println("*3");
        
        bio = partitions[partition];
    }    
    // register as DEP
    naming.registerPortal(bio, bioName);
    Debug.out.println("Block I/O device registered as "+bioName);
    
    }
}

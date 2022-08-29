package bioide;

import jx.InitialNaming;
import jx.devices.bio.BlockIO;
import jx.zero.*;

public class Main {
    public static void main(String [] args) {
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
    InitialNaming.registerPortal(bio, bioName);
    Debug.out.println("Block I/O device registered as " + bioName);
    }
}

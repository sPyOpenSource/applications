package test.ide;

import bioide.IDEDeviceImpl;
import bioide.Drive;
import bioide.PartitionEntry;
import jx.devices.bio.BlockIO;

import jx.zero.*;
import jx.zero.debug.*;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class IDEDomain {
    Naming naming;

    public static void init(Naming naming) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("Domain IDEDomain speaking.");
	cpuManager.setThreadName("IDEDomain-Main");
	new IDEDomain(naming);
    }
    
    IDEDomain(final Naming naming) {
	this.naming = naming;	    

	IDEDeviceImpl ide = new IDEDeviceImpl();
	Drive[] drives = ide.getDrives();
	PartitionEntry[] partitions = drives[0].getPartitions();
	
      final BlockIO portal = partitions[2];
      
      // register as DEP
      naming.registerPortal(portal, "BlockIO");
    }
}

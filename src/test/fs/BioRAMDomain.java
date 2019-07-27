package test.fs;

import jx.zero.*;
import jx.zero.debug.*;
import bioram.BlockIORAM;
import jx.init.InitNaming;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

public class BioRAMDomain {
    Naming naming;

    public static void main(String [] args) {
	Naming naming = InitialNaming.getInitialNaming();
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	cpuManager.setThreadName("BioRAM");
	String bioName = args[0];
	new BioRAMDomain(naming, bioName);
    }

    public static void init(Naming naming, String [] args) {
	DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
	CPUManager cpuManager = (CPUManager) naming.lookup("CPUManager");
	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
	Debug.out.println("BioRAMDomain speaking.");
	
	String bioName = args[0];
	new BioRAMDomain(naming, bioName);
    }
    BioRAMDomain(final Naming naming, String name) {
	this.naming = naming;	
	final BlockIORAM bio = new BlockIORAM(20 * 1024);
	InitNaming.registerPortal(bio, name);
	Debug.out.println("Block I/O device registered as " + name);
    }
}

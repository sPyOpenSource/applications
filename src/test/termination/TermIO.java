package test.termination;

import jx.zero.*;

public class TermIO {
    static MemoryManager memMgr;
    static Naming naming;
    static DomainManager dm;
    public static void main(String []args) {
	naming = InitialNaming.getInitialNaming();
	memMgr = (MemoryManager)naming.lookup("MemoryManager");
	dm = (DomainManager)naming.lookup("DomainManager");

	int mem_before = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory before domain started: "+mem_before);

	Debug.out.println ("Starting test domain ...\n");

	Domain bio = DomainStarter.createDomain("BIO",
					     "test_fs.jll", 
					     "test/fs/BioRAMDomain",
					     2*1024*1024,
						new String[] {"BIOFS"});

	Debug.out.println ("Free memory when domain running: "+memMgr.getTotalFreeMemory());

	for(int i=0;i<1000;i++) Thread.yield();

	dm.terminate(bio);

	while (! bio.isTerminated()) Thread.yield();

	int mem_term = memMgr.getTotalFreeMemory();
	Debug.out.println ("Free memory when domain terminated: "+mem_term);
	Debug.out.println ("    Difference: "+(mem_before - mem_term));
    }
}

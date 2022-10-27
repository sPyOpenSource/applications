/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI;

import jx.bio.BlockIO;
import jx.zero.Clock;
import jx.zero.Naming;
import jx.fs.javafs.FileSystem;
import test.debug.Monitor;
import test.fs.BioRAMDomain;

/**
 *
 * @author X. Wang
 */
public class POST {
    private final Naming naming;
    
    public POST(Naming naming){
        this.naming = naming;
    }
    
    public void test(){
        //test.fb.Main.init(naming, null);
        Monitor.init(naming, null);
        //test.gc.Main.init(naming);
        BioRAMDomain.init(naming, new String[]{"BioRAM"});
        FileSystem jfs = new FileSystem();
        BlockIO bio = (BlockIO)naming.lookup("BioRAM");
        Clock clock = (Clock)naming.lookup("");
        jfs.init(bio, new jx.bio.buffercache.BufferCache(bio, clock, 800, 1000, 100, 512), clock);
        naming.registerPortal(jfs, "JavaFS");
        /*FSImpl fs = new FSImpl();
        fs.mountRoot(jfs, false);
        naming.registerPortal(fs, "FS");
        test.fs.ReRead.main(new String[]{"FS"});*/
    }
}

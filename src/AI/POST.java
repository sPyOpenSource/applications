/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI;

import jx.bio.BlockIO;
import jx.zero.Clock;
import jx.zero.Naming;
import test.debug.Monitor;
import test.fs.BioRAMDomain;
import vfs.FSImpl;

/**
 *
 * @author spy
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
        javafs.FileSystem jfs = new javafs.FileSystem();
        BlockIO bio = (BlockIO)naming.lookup("BioRAM");
        Clock clock = (Clock)naming.lookup("");
        jfs.init(bio, new buffercache.BufferCache(bio, clock, 800, 1000, 100, 512), clock);
        naming.registerPortal(jfs, "JavaFS");
        /*FSImpl fs = new FSImpl();
        fs.mountRoot(jfs, false);
        naming.registerPortal(fs, "FS");
        test.fs.ReRead.main(new String[]{"FS"});*/
    }
}

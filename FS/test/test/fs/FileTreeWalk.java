package test.fs;

import jx.zero.*;
import jx.zero.debug.*;
import jx.fs.FS;
import jx.fs.Node;
import jx.fs.FSException;

public class FileTreeWalk {

    Naming naming;

    public static void init(Naming naming) {
    try {
        if (Debug.out==null) {
        DebugChannel d = (DebugChannel) naming.lookup("DebugChannel0");
        Debug.out = new DebugPrintStream(new DebugOutputStream(d));
        }
        new FileTreeWalk(naming);
    } catch(Exception e) {
        //e.printStackTrace();
        throw new Error();
    }
    }

    public FileTreeWalk(Naming naming) throws Exception {
    this.naming = naming;

    FS fs = (FS) naming.lookup("FS");

    if (fs == null) {
        if (Debug.out != null) Debug.out.println("FS not found!");        
        return;
    }

    printDir(" ", fs.getCwdNode());

    }

    private void printDir(String space, Node dirInode) throws Exception {
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
            Debug.out.print("  \t\t"+name);
            if (inode.isSymlink())
                Debug.out.print(" -> " + inode.getSymlink());
            Debug.out.println();
        }
        for (String name : names) {
            inode = dirInode.lookup(name);
            if (! name.equals(".") && ! name.equals("..") && inode.isDirectory()) {
                Debug.out.println(space+"-------------");
                Debug.out.println(space+name+":");
                printDir(space + "  ", inode);
            }
            inode.decUseCount();
        }
    }

}

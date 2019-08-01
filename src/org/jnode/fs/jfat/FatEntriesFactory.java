package org.jnode.fs.jfat;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jx.zero.Debug;
import jx.zero.InitialNaming;
import jx.zero.Memory;
import jx.zero.MemoryManager;
import jx.zero.Naming;
//import org.apache.log4j.Logger;

public class FatEntriesFactory {

    //private static final Logger log = Logger.getLogger(FatEntriesFactory.class);

    private boolean label;
    private int index;
    private int next;
    private FatEntry entry;
    protected boolean includeDeleted;
    private FatDirectory directory;
private Naming naming = InitialNaming.getInitialNaming();
    public FatEntriesFactory(FatDirectory directory, boolean includeDeleted) {
        label = false;
        index = 0;
        next = 0;
        entry = null;
        this.includeDeleted = includeDeleted;
        this.directory = directory;
    }

    protected boolean hasNextEntry() {
        int i;
        FatDirEntry e;
        FatRecord v = new FatRecord();

  /*      if (index > FatDirectory.MAXENTRIES)
            log.debug("Full Directory: invalid index " + index);
*/
        for (i = index;; ) {
                /*
                 * create a new entry from the chain
                 */
            try {
                Debug.out.println(i);
                e = directory.getFatDirEntry(i, includeDeleted);
                i++;
            } catch (NoSuchElementException ex) {
                entry = null;
                return false;
            } catch (IOException ex) {
                //log.debug("cannot read entry " + i);
                i++;
                continue;
            }
            break;
            /*if (e.isFreeDirEntry() && e.isLongDirEntry() && includeDeleted) {
                Ignore damage on deleted long directory entries
                ((FatLongDirEntry) e).setDamaged(false);
            }

            if (e.isFreeDirEntry() && !includeDeleted) {
                v.clear();
            } else if (e.isLongDirEntry()) {
                FatLongDirEntry l = (FatLongDirEntry) e;
                if (l.isDamaged()) {
                    //log.debug("Damaged entry at " + (i - 1));
                    v.clear();
                } else {
                    v.add(l);
                }
            } else if (e.isShortDirEntry()) {
                FatShortDirEntry s = (FatShortDirEntry) e;
                if (s.isLabel()) {
                    if (directory.isRoot()) {
                        FatRootDirectory r = (FatRootDirectory) directory;
                        if (label) {
                            //log.debug("Duplicated label in root directory");
                        } else {
                            r.setEntry(s);
                            label = true;
                        }
                    } else {
                        //log.debug("Volume label in non root directory");
                    }
                } else {
                    break;
                }
            } else if (e.isLastDirEntry()) {
                entry = null;
                return false;
            } else
                throw new UnsupportedOperationException(
                    "FatDirEntry is of unknown type, shouldn't happen");*/
        }

  /*      if (!e.isShortDirEntry())
            throw new UnsupportedOperationException("shouldn't happen");
*/
        v.close((FatShortDirEntry)e);

            /*
             * here recursion is in action for the entries factory it creates
             * directory nodes and file leafs
             */
        //if (e.isDirectory())
          //  this.entry = new FatDirectory(directory.getFatFileSystem(), directory, v);
        //else
        FatFile file = new FatFile(directory.getFatFileSystem(), directory, v);
        this.entry = file;
        MemoryManager memoryManager = (MemoryManager)naming.lookup("MemoryManager");
        Memory buffer = memoryManager.allocAligned(512, 8);
        try {
            file.read(0, buffer);
            for(int j = 0; j < 30; j++){
                Debug.out.print((char)buffer.get8(j));
            }
            Debug.out.println();
        } catch (IOException ex) {
           // Logger.getLogger(FatEntriesFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.next = i;
if(i > 3) return false;
       return true;
    }

    public FatEntry createNextEntry() {
        if (index == next)
            hasNextEntry();
        if (entry == null)
            throw new NoSuchElementException();
        index = next;
        return entry;
    }
}
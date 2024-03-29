package vfs;

import java.util.Hashtable;
import jx.zero.Debug;
import jx.zero.Service;
import jx.zero.Memory;
import jx.fs.Inode;
import jx.fs.FileSystem;
import jx.fs.FS;
import jx.fs.FSException;
import jx.fs.PermissionException;
import jx.fs.*;

public class FSImpl implements FS, Service {
    private Inode         rootInode;
    private FileSystem    rootFS;
    private Inode         cwdInode;
    private String        cwdPath;
    private final Hashtable     mountpoints;
    private final Hashtable     devices; // maps ID to FileSystem
    private final DirEntryCache direntrycache;

    public FSImpl() {
	cwdPath = "/";
	mountpoints = new Hashtable();
	direntrycache = DirEntryCache.instance();
	rootInode = null;
	devices = new Hashtable();
    }

    @Override
    public final String getCwdPath() {
	return cwdPath;
    }

    @Override
    public final Inode getCwdInode() {
	//cwdInode.incUseCount(); // oder nicht?
	return cwdInode;
    }

    private Inode igetCwdInode() {
	cwdInode.incUseCount();
	return cwdInode;
    }

    @Override
    public final void cleanUp() throws InodeIOException,NotExistException {
	cwdInode.decUseCount();
	rootInode.decUseCount();
	direntrycache.syncEntries();
	rootFS.release();
	direntrycache.invalidateEntries();
	//buffercache.syncDevice(0); nicht mehr noetig
    }

    @Override
    public final void mount(FileSystem filesystem, String path, boolean read_only) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	Inode pi = null;
	if (isPath(path))
	    pi = lookup(getPathName(path));
	
	direntrycache.removeEntry(getAbsolutePath(path));
	filesystem.init(read_only);
	Inode mnt = filesystem.getRootInode();
	direntrycache.addEntry(getAbsolutePath(path), mnt);
	if (isPath(path)) {
	    pi.overlay(mnt, getFileName(path));
	    mountpoints.put(filesystem, pi);
	    pi.decUseCount();
	} else {
	    cwdInode.overlay(mnt, getFileName(path));
	    mountpoints.put(filesystem, igetCwdInode());
	}
	devices.put(new Integer(filesystem.getDeviceID()), filesystem);
	//devices.put(filesystem.getDeviceID(), filesystem);
    }

    @Override
    public final void unmount(FileSystem filesystem) throws InodeNotFoundException, NoDirectoryInodeException, NotExistException {
	Inode overlayedInode = (Inode)mountpoints.get(filesystem);
	if (overlayedInode == null)
	    return;
	Inode root = filesystem.getRootInode();
	//direntrycache.removeEntry(getAbsolutePath()); TODO!
	overlayedInode.removeOverlay(root);
	overlayedInode.decUseCount();
	root.decUseCount();
	filesystem.release();
	direntrycache.invalidateEntries();
	mountpoints.remove(filesystem);
    }

    @Override
    public final void mountRoot(FileSystem filesystem, boolean read_only) {
	if (rootInode != null)
	    rootInode.decUseCount();
	if (cwdInode != null)
	    cwdInode.decUseCount();
	filesystem.init(read_only);
	rootInode = filesystem.getRootInode();
	cwdInode = filesystem.getRootInode();
	direntrycache.addEntry("/", rootInode);
	cwdPath = "/";
	mountpoints.put(filesystem, rootInode);
	rootFS = filesystem;
	devices.put(new Integer(filesystem.getDeviceID()), filesystem);	
	//devices.put(filesystem.getDeviceID(), filesystem);
    }

    @Override
    public final int available() throws NotExistException {
	return rootInode.available();
    }

    @Override
    public final void cd(String path) {
	String tmpPath = null;
	Inode tmpInode = null;

	try {
	    if (path.equals("."))
		return;
	    if (path.equals(".."))
		tmpPath = getPathName(cwdPath);
	    else
		tmpPath = getAbsolutePath(path);
	    tmpInode = lookup(tmpPath);
	} catch (InodeIOException e) {
	    Debug.out.println("cd: Fehler beim Lesen von '" + path + "'");
	    return;
	} catch (InodeNotFoundException e) {
	    Debug.out.println("cd: '" + path + "' existiert nicht");
	    return;
	} catch (NoDirectoryInodeException e) {
	    Debug.out.println("cd: '" + path + "' ist kein Verzeichnis");
	    return;
	} catch (NotExistException e) {
	    Debug.out.println("cd: '" + path + "' ist nicht mehr gltig");
	    return;
	} catch (PermissionException e) {
	    Debug.out.println("cd: Zugriff auf '" + path + "' nicht erlaubt");
	    return;
	}
	cwdPath = tmpPath;
	if (cwdInode != null) // && cwdInode != rootInode)
	    cwdInode.decUseCount();
	cwdInode = tmpInode;
    }

    @Override
    public final void rename(String path, String pathneu) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	try {
	    Inode pi, pineu;
	    
	    if (path.equals("/")) // ein Rootverzeichnis kann nicht verschoben werden
		throw new PermissionException();
	    if (isPath(path))
		pi = lookup(getPathName(path));
	    else
		pi = igetCwdInode();
	    if (pi.isOverlayed(getFileName(path))) {
		pi.decUseCount();
		throw new PermissionException(); // ein Mountpunkt kann nicht verschoben werden
	    }
	    if (isPath(pathneu))
		pineu = lookup(getPathName(pathneu));
	    else
		pineu = igetCwdInode();
	    pi.rename(getFileName(path), pineu, getFileName(pathneu));
	    direntrycache.moveEntry(getAbsolutePath(path), getAbsolutePath(pathneu));
	    
	    pi.decUseCount();
	    pineu.decUseCount();
 	} catch (FSException e) { } // alle Exceptions ignorieren
    }

    @Override
    public final void symlink(String path, String pathneu) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,NotSupportedException, PermissionException {
	Inode pi;
	
	if (isPath(pathneu))
	    pi = lookup(getPathName(pathneu));
	else
	    pi = igetCwdInode();
	Inode newinode = pi.symlink(getAbsolutePath(path), getFileName(pathneu));
	pi.decUseCount();
	direntrycache.addEntry(getAbsolutePath(pathneu), newinode);
	newinode.decUseCount();
    }

    @Override
    public final void mkdir(String path, int mode) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException {
	Inode newdir;
	if (isPath(path)) {
	    Inode pi = lookup(getPathName(path));
	    newdir = pi.mkdir(getFileName(path), mode);
	    pi.decUseCount();
	} else {
	    newdir = cwdInode.mkdir(path, mode);
	}
	direntrycache.addEntry(getAbsolutePath(path), newdir);
	newdir.decUseCount();
    }

    @Override
    public final void rmdir(String path) throws DirNotEmptyException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException {
	direntrycache.removeEntry(getAbsolutePath(path)); // ruft schliesslich auch decUseCount() auf
	if (isPath(path)) {
	    Inode pi = lookup(getPathName(path));
	    pi.rmdir(getFileName(path));
	    pi.decUseCount();
	} else {
	    cwdInode.rmdir(path);
	}
    }
    
    @Override
    public final void create(String path, int mode) throws FileExistsException, InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException,PermissionException {
	Inode newfile;
	if (isPath(path)) {
	    Inode pi = lookup(getPathName(path));
	    newfile = pi.create(getFileName(path), mode);
	    pi.decUseCount();
	} else {
	    newfile = cwdInode.create(path, mode);
	}
	direntrycache.addEntry(getAbsolutePath(path), newfile);
	newfile.decUseCount();
    }

    @Override
    public final void unlink(String path) throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NoFileInodeException, NotExistException,PermissionException {
	direntrycache.removeEntry(getAbsolutePath(path));
	if (isPath(path)) {
	    Inode pi = lookup(getPathName(path));
	    pi.unlink(getFileName(path));
	    pi.decUseCount();
	} else {
	    cwdInode.unlink(path);
	}
    }

    @Override
    public final String getPathName(String path) {
	int n = path.lastIndexOf('/');
	if (n == 0) return "/";
	if (n == -1) return cwdPath;
	else return path.substring(0, n);
    }

    @Override
    public final String getFileName(String path) {
	int n = path.lastIndexOf('/');
	if (n == -1)
	    return path;
	return path.substring(n+1);
    }

    @Override
    public final Inode lookup(String path)throws InodeIOException, InodeNotFoundException, NoDirectoryInodeException, NotExistException, PermissionException {
	Inode inode = direntrycache.getEntry(getAbsolutePath(path));
	if (inode != null)  // Eintrag im Cache
	    return inode;
	
	if (isPath(path) == false)
	    inode = cwdInode.lookup(path);
	else {
	    path = getAbsolutePath(path);
	    if (path.equals("/")) {
		rootInode.incUseCount();
		return rootInode;
	    }
	    Inode pi = lookup(getPathName(path));
	    inode = pi.lookup(getFileName(path));
	    //if (pi != rootInode) TODO: CHECK!!!
	    pi.decUseCount();
	}
	if (inode == null) throw new InodeNotFoundException();
	if (inode.isSymlink()) {
	    Debug.out.println("Symlink!");
	    String symlink = null;
	    try {
		symlink = inode.getSymlink();
	    } catch (NoSymlinkInodeException | NotSupportedException e) {
		throw new InodeIOException();
	    }
	    inode.decUseCount();
	    return lookup(symlink);
	}
	direntrycache.addEntry(getAbsolutePath(path), inode);

	return inode;
    }
    
    private String getAbsolutePath(String name)
    {
	if (isAbsolute(name))
	    return name;
	if (cwdPath.charAt(cwdPath.length()-1) == '/')
	    return cwdPath + name;
	else
	    return cwdPath + '/' + name;
    }

    @Override
    public final int read(String path, Memory m, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	throw new Error();
    }

    @Override
    public final int write(String path, Memory m, int off, int len) throws InodeIOException, NoFileInodeException, NotExistException, PermissionException {
	throw new Error();
    }

    @Override
    public Inode getInode(int deviceIdentifier, int identifier) throws FSException, NotExistException, PermissionException{
       FileSystem filesystem = (FileSystem) devices.get(deviceIdentifier);
       
       if (filesystem == null) Debug.out.println("filesystem ist null");
       filesystem.getInode(identifier);

       return filesystem.getInode(identifier);
    }

    @Override
    public boolean isPath(String name) {
        return FS.super.isPath(name); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isAbsolute(String name) {
        return FS.super.isAbsolute(name); //To change body of generated methods, choose Tools | Templates.
    }
}

package jx.fs;

public interface FSObject {
    public FileSystemInterface getFileSystem() throws Exception;
    public boolean isFile() throws Exception;
    public boolean isDirectory() throws Exception;
    public Permission getPermission() throws Exception;
    public FSAttribute getAttribute() throws Exception;
    public void close() throws Exception;
    public int getLength() throws Exception;
    public boolean isValid();
}

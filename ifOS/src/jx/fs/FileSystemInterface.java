package jx.fs;

public interface FileSystemInterface extends jx.zero.Portal {
    public String     getName();
    public Permission getDefaultPermission();

    public FSObject   openRootDirectoryRO() throws Exception;
    public FSObject   openRootDirectoryRW() throws Exception;

    public void   mount() throws Exception;
    public void   unmount() throws Exception;
}

package jx.fs;

public interface ReadOnlyDirectory extends FSObject, jx.zero.Portal {
    public String[]   list() throws Exception;
    public FSObject   openRO(String name) throws Exception;
    public FSObject   openRW(String name) throws Exception;
}

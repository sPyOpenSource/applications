package jx.classstore;

import jx.classfile.ClassData;

public interface ClassFinder {
    public ClassData findClass(String className);
    public void dump();
}

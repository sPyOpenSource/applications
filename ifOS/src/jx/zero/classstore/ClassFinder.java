package jx.zero.classstore; 

import jx.zero.collections.Iterator;

public interface ClassFinder {
    public ClassData findClass(String className);
    public void dump();
    public Iterator getAllClasses();

    /**
     * @return true if instances of "className" can be assigned to
     * variables of type superName
     */
    // public boolean isAssignableTo(String className, String superName);
}

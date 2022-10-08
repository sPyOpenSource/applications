
package jx.fs;

/**
 *
 * @author xuyi
 */
public interface StatFS {
    public int tsize = 0;
    public int bsize = 0; 
    public int blocks = 0; // total blocks
    public int bfree = 0;  // free blocks
    public int bavail = 0; // available blocks
}

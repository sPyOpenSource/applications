
package jx.fs;

/**
 *
 * @author xuyi
 */
public interface StatFS {
    public void setSize(int size);
    public int getSize(); 
    public int blocks = 0; // total blocks
    public int bfree = 0;  // free blocks
    public int bavail = 0; // available blocks

    public int getBlockSize();

    public void setBlockSize(int s_blocksize);

    public void setBlocks(int numberOfBlocks);

    public void setFreeBlocks(int numberOfFreeBlocks);
}

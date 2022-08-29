
package jx.fs.buffer;

/**
 *
 * @author xuyi
 */
public interface BufferCache {

    public void syncDevice(boolean b);

    public void flushCache();

    public void showBuffers();

    public BufferHead bread(int bg_inode_table);

    public BufferHead getblk(int nr);

    public void bdwrite(BufferHead bh);

    public void brelse(BufferHead bh);
    
}


package jx.net;

/**
 *
 * @author xuyi
 */
public interface IPAddress {
    public int addr = 0;

    public byte[] bytes = new byte[4];

    public int getAddress();

    public byte[] getBytes();

    public String getHostName();
    
}

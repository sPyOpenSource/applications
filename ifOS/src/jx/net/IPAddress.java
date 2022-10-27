
package jx.net;

/**
 *
 * @author xuyi
 */
public interface IPAddress {

    public byte[] bytes = new byte[4];

    public int getAddress();

    public byte[] getBytes();

    public String getHostName();
    
}

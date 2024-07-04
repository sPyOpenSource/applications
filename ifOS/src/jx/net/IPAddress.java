
package jx.net;

import java.net.InetAddress;

/**
 *
 * @author xuyi
 */
public interface IPAddress {
    
    public IPAddress getDefaultSubnetmask();

    public int getAddress();

    public byte[] getBytes();

    public String getHostName();

    public InetAddress toInetAddress();
    
    public boolean isAnyLocalAddress();

    public IPAddress and(IPAddress defaultSubnetmask);
    
}

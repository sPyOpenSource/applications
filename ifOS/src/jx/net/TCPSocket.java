package jx.net;

import java.io.IOException;
import jx.zero.Memory;

public interface TCPSocket extends jx.zero.Portal {
    public Memory processTCP(IPData m);
    public Memory processTCPServer(IPData m); 
    public void processClientSocket();       // call periodically for retransmissions ... 
    public TCPSocket accept(Memory[] newbufs) throws IOException;
    public void close() throws IOException;
    public void open(IPAddress remoteIP, int remotePort) throws Exception;
    //public void send(byte data);
    public byte[] readFromInputBuffer();
    public void send(byte[] byteArr) throws IOException;
}

package jx.net;

import jx.zero.Memory;

public interface TCPSocket extends jx.zero.Portal {
    public Memory processTCP(IPData m);
    public Memory processTCPServer(IPData m); 
    public void processClientSocket();       // call periodically for retransmissions ... 
    public TCPSocket accept(Memory[] newbufs);
    public void close();
    public void open(IPAddress remoteIP, int remotePort);
    //public void send(byte data);
    public byte[] readFromInputBuffer();
    public void send(byte[] byteArr);
}

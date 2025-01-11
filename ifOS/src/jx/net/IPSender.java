package jx.net;

import jx.zero.Memory;

public interface IPSender extends jx.zero.Portal {
    Memory send(Memory m);
    Memory send(Memory m, int offset, int size);
    void close();
    IPAddress getDestination();
}

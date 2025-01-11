package jx.net;

import jx.fs.buffer.separator.MemoryConsumer;

public interface PacketsProducer {
    public boolean registerConsumer(MemoryConsumer consumer, String name);
    // public IPAddress getSource(Memory buf);
}

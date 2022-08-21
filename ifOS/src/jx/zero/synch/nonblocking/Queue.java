package jx.zero.synch.nonblocking;

public interface Queue {
    void enqueue(Object value);
    Object dequeue();
}

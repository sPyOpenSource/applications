package jx.zero.synch.mutex;

public interface BlockingLock {
    void lock();
    void unlock();
}

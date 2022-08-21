package jx.fs.buffer.multithread;

/**
 * The operations that are available to the consumer of buffers.
 */
public interface BufferConsumer {
    public Buffer undockFirstElement();
}

package jx.net.rpc;

public interface RPCReader {
  public Object read(byte[] buf, int offset);
}

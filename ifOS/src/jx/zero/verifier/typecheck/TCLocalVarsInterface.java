package jx.zero.verifier.typecheck;

public interface TCLocalVarsInterface {
    public void write(int index, TCTypes type, int bcAddr);    
    public TCTypes read(int index, TCTypes type);    
}

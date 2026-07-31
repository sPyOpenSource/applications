package jx.verifier.typecheck;

import jx.verifier.VerifyException;

public interface TCLocalVarsInterface {
    public void write(int index, TCTypes type, int bcAddr) throws VerifyException;
    public TCTypes read(int index, TCTypes type) throws VerifyException;
}

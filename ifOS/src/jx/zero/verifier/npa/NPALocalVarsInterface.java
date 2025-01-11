package jx.zero.verifier.npa;

public interface NPALocalVarsInterface {
    public void write(int index, NPAValue type, int bcAddr);
    public NPAValue NPAread(int index);
    //find all local Vars with same id as value (must be a valid id!) and change their
    //value to newVal.
    public void setValue(NPAValue value, int newVal);
}

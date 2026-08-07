package jx.zero.verifier.typecheck;

/**
 * @deprecated Implement {@link jx.zero.verifier.TypeCheckStrategy} instead.
 */
@Deprecated
public interface TCLocalVarsInterface {
    public void write(int index, TCTypes type, int bcAddr);    
    public TCTypes read(int index, TCTypes type);    
}

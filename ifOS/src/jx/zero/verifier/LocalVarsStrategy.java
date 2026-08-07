package jx.zero.verifier;

import jx.zero.verifier.npa.NPAValue;

/**
 * Strategy for reading and writing local variables during
 * non-pointer-array (NPA) verification.
 *
 * <p>Replaces {@code jx.zero.verifier.npa.NPALocalVarsInterface}.
 */
public interface LocalVarsStrategy {
    void write(int index, NPAValue type, int bcAddr);

    NPAValue read(int index);

    void setValue(NPAValue value, int newVal);
}

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

    /**
     * Reads the value of the local variable at {@code index}.
     *
     * <p>Behavior for an uninitialized slot is implementation-specific:
     * implementations may return a default {@code NPAValue} or throw an
     * unchecked exception.
     */
    NPAValue read(int index);

    /**
     * Changes the value of every local variable sharing the same id as
     * {@code value} to {@code newVal}.
     *
     * <p>{@code value} must carry a valid id.
     */
    void setValue(NPAValue value, int newVal);
}

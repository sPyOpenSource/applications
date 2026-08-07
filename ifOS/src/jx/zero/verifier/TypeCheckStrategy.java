package jx.zero.verifier;

import jx.zero.verifier.typecheck.TCTypes;

/**
 * Strategy for reading and writing local variables during type-check
 * verification.
 *
 * <p>Replaces {@code jx.zero.verifier.typecheck.TCLocalVarsInterface}.
 */
public interface TypeCheckStrategy {
    void write(int index, TCTypes type, int bcAddr);

    TCTypes read(int index, TCTypes type);
}

package jx.zero.verifier;

import jx.zero.ByteCode;
import jx.zero.classfile.MethodSource;

/**
 * Base class for bytecode verifiers.
 *
 * <p>Holds the verifier's shared state (method, subroutine set, user
 * parameter) and drives the check lifecycle via {@link #runChecks()}:
 * iterate the bytecodes from {@link #getByteCodes()}, call
 * {@link #checkBC(ByteCode)} for each, then call {@link #endChecks()}.
 *
 * <p>Implements the legacy {@link VerifierInterface} for backward
 * compatibility.
 */
// implements deprecated VerifierInterface for backward compatibility
@SuppressWarnings("deprecation")
public abstract class AbstractVerifier implements VerifierInterface {
    protected final MethodSource method;
    protected final Subroutines srs;
    protected final Object parameter;

    /**
     * Creates a verifier for the given method.
     */
    protected AbstractVerifier(MethodSource method, Subroutines srs, Object parameter) {
        this.method = method;
        this.srs = srs;
        this.parameter = parameter;
    }

    /**
     * Returns the method being verified.
     */
    public MethodSource getMethod() {
        return method;
    }

    /**
     * Returns the subroutine set of the method being verified.
     */
    public Subroutines getSrs() {
        return srs;
    }

    /**
     * Returns the user-defined parameter, or {@code null} if none was set.
     */
    public Object getParameter() {
        return parameter;
    }

    /**
     * Runs the full check lifecycle: checks every bytecode from
     * {@link #getByteCodes()} and then calls {@link #endChecks()}, which
     * runs even if a check throws.
     */
    public final void runChecks() {
        try {
            for (ByteCode code : getByteCodes()) {
                checkBC(code);
            }
        } finally {
            endChecks();
        }
    }

    /**
     * Returns the bytecodes of the method to verify.
     *
     * <p>Must not return {@code null}; an empty array is allowed.
     */
    protected abstract ByteCode[] getByteCodes();

    /**
     * Checks a single bytecode.
     */
    public abstract void checkBC(ByteCode e);

    /**
     * Returns the name of the class being verified.
     */
    public abstract String getClassName();

    /**
     * Completes the checks (e.g. report summary, reset state).
     */
    public abstract void endChecks();
}

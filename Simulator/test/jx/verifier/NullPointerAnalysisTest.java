package jx.verifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import jx.classfile.ClassData;
import jx.classfile.MethodData;
import jx.classfile.VerifyResult;
import jx.verifier.npa.NPAResult;

/**
 * Tests for the null-pointer analysis ({@code jx.verifier.npa}).
 * The analysis is a second abstract-interpretation pass over the same
 * {@link MethodVerifier} engine; it must prove that a dereference guarded by
 * a null check no longer needs a runtime check.
 */
public class NullPointerAnalysisTest {

    private static final String C = "jx/verifier/test/Cleanup";

    @Test
    public void derefAfterNullGuardIsProvenNonNull() throws IOException {
        // aload_0; ifnonnull +5; iconst_0; ireturn; aload_0; arraylength; ireturn
        //   a == null → return 0
        //   a != null → return a.length   (arraylength at address 7)
        byte[] code = {
            0x2a,                 // 0: aload_0
            (byte) 0xc7, 0x00, 5, // 1: ifnonnull +5 → 6
            0x03,                 // 4: iconst_0
            (byte) 0xac,          // 5: ireturn
            0x2a,                 // 6: aload_0
            (byte) 0xbe,          // 7: arraylength
            (byte) 0xac           // 8: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "len", "([I)I", code, 1, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.nullPointerAnalysis(VerifierTestUtil.finder(b), cd, "len", "([I)I");

        MethodData m = cd.getMethodData("len", "([I)I");
        NPAResult result = (NPAResult) m.getVerifyResult(VerifyResult.NPA_RESULT);
        assertTrue("arraylength at address 7 should be provably non-null", result.notNull(7));
    }

    @Test
    public void unguardedDerefIsNotProvenNonNull() throws IOException {
        // aload_0; arraylength; ireturn  (no null check anywhere)
        byte[] code = {
            0x2a,        // 0: aload_0
            (byte) 0xbe, // 1: arraylength
            (byte) 0xac  // 2: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "rawLen", "([I)I", code, 1, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.nullPointerAnalysis(VerifierTestUtil.finder(b), cd, "rawLen", "([I)I");

        MethodData m = cd.getMethodData("rawLen", "([I)I");
        NPAResult result = (NPAResult) m.getVerifyResult(VerifyResult.NPA_RESULT);
        assertFalse("unguarded deref must not be marked non-null", result.notNull(1));
    }

    @Test
    public void derefOnDefinitelyNullPathIsFlagged() throws IOException {
        // aconst_null; astore_0; aload_0; arraylength; ireturn
        //   arraylength at address 3 operates on a provably-null reference
        byte[] code = {
            0x01,        // 0: aconst_null
            0x3b,        // 1: astore_0
            0x2a,        // 2: aload_0
            (byte) 0xbe, // 3: arraylength
            (byte) 0xac  // 4: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "badLen", "([I)I", code, 1, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.nullPointerAnalysis(VerifierTestUtil.finder(b), cd, "badLen", "([I)I");

        MethodData m = cd.getMethodData("badLen", "([I)I");
        NPAResult result = (NPAResult) m.getVerifyResult(VerifyResult.NPA_RESULT);
        assertFalse("deref on a provably-null reference must not be marked non-null",
                result.notNull(3));
    }
}

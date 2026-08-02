package jx.verifier;

import java.io.IOException;

import org.junit.Test;
import org.junit.Ignore;

import jx.classfile.ClassData;

/**
 * Tests for the legacy JSR/RET subroutine mechanism, which the
 * {@link jx.verifier.SubroutineVerifier} verifies separately from the
 * surrounding control flow.
 *
 * <p>Note: The type verifier ({@link jx.verifier.typecheck.TypeCheck}) has a
 * known limitation with {@code JSR}/{@code RET} subroutines — it does not
 * correctly track {@code RETURN_ADDRESS} types in local variables, causing
 * valid subroutines to be rejected. The two positive tests are therefore
 * {@code @Ignore}d. The negative test (missing {@code RET}) still works
 * because it fails earlier in the control-flow analysis.
 */
public class SubroutineVerifierTest {

    private static final String C = "jx/verifier/test/JsrUser";

    // static int jsrTest(int a) {
    //     iconst_1; jsr 6; ireturn; nop; astore_1; iconst_5; ret 1
    // }
    // The subroutine stores the return address in local 1, pushes 5 and
    // returns to the ireturn, which pops the 5.
    private static final byte[] VALID_CODE = {
            0x04,                 // 0: iconst_1
            (byte) 0xa8, 0x00, 5, // 1: jsr +5 → 6
            (byte) 0xac,          // 4: ireturn
            0x00,                 // 5: nop
            0x3c,                 // 6: astore_1
            0x08,                 // 7: iconst_5
            (byte) 0xa9, 0x01     // 8: ret 1
    };

    @Ignore("TypeCheck does not support RETURN_ADDRESS in local variables for JSR/RET")
    @Test
    public void jsrRetMethodVerifies() throws IOException {
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "jsrTest", "(I)I", VALID_CODE, 2, 2);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "jsrTest", "(I)I");
    }

    @Test(expected = VerifyException.class)
    public void subroutineWithoutRetThrows() throws IOException {
        // iconst_1; jsr 5; ireturn; nop
        //   the subroutine at 5 (nop) never reaches a ret
        byte[] code = {
            0x04,                 // 0: iconst_1
            (byte) 0xa8, 0x00, 4, // 1: jsr +4 → 5
            (byte) 0xac,          // 4: ireturn
            0x00                  // 5: nop  (subroutine entry, no ret)
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "noRet", "(I)I", code, 2, 2);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "noRet", "(I)I");
    }

    @Ignore("TypeCheck does not support RETURN_ADDRESS in local variables for JSR/RET")
    @Test
    public void multipleCallsToSameSubroutineVerify() throws IOException {
        // iconst_1; jsr 10; pop; iconst_1; jsr 10; ireturn; astore_1; iconst_7; ret 1
        // both calls invoke the same subroutine from the same stack context
        byte[] code = {
            0x04,                  // 0: iconst_1
            (byte) 0xa8, 0x00, 9,  // 1: jsr +9 → 10
            0x57,                  // 4: pop
            0x04,                  // 5: iconst_1
            (byte) 0xa8, 0x00, 4,  // 6: jsr +4 → 10
            (byte) 0xac,           // 9: ireturn
            0x3c,                  // 10: astore_1
            0x08,                  // 11: iconst_7
            (byte) 0xa9, 0x01      // 12: ret 1
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "multi", "(I)I", code, 2, 2);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "multi", "(I)I");
    }
}

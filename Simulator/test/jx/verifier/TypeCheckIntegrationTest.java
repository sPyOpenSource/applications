package jx.verifier;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import jx.classfile.ClassData;
import jx.verifier.typecheck.TypeCheck;

/**
 * End-to-end tests for the type verifier ({@link TypeCheck}). Uses
 * hand-built class files so we can feed the verifier both valid methods and
 * bytecode that javac would never produce — stack underflow, mismatched
 * operand types, and control-flow merges with unequal stack heights.
 */
public class TypeCheckIntegrationTest {

    private static final String C = "jx/verifier/test/Calc";

    // --- valid methods must verify cleanly -----------------------------

    @Test
    public void staticIntArithmeticPasses() throws IOException {
        // iload_0; iload_1; iadd; ireturn
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "add", "(II)I",
                    new byte[]{0x1a, 0x1b, 0x60, (byte) 0xac}, 2, 2);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "add", "(II)I");
    }

    @Test
    public void localVarRoundTripPasses() throws IOException {
        // iconst_5; istore_0; iload_0; ireturn
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "constFive", "()I",
                    new byte[]{0x08, 0x3b, 0x1a, (byte) 0xac}, 1, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "constFive", "()I");
    }

    @Test
    public void controlFlowWithMergePasses() throws IOException {
        // iload_0; iconst_0; if_icmpgt +7; iconst_0; goto +4; iload_0; ireturn
        // Path A (x <= 0): iconst_0 → goto merge
        // Path B (x > 0):  iload_0 → merge
        // Merge at ireturn: both stacks have exactly one int
        byte[] code = {
            0x1a,                 // 0: iload_0
            0x03,                 // 1: iconst_0
            (byte) 0xa3, 0x00, 7, // 2: if_icmpgt +7 → 9
            0x03,                 // 5: iconst_0
            (byte) 0xa7, 0x00, 4, // 6: goto +4 → 10
            0x1a,                 // 9: iload_0
            (byte) 0xac           // 10: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "clamp", "(I)I", code, 2, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "clamp", "(I)I");
    }

    @Test
    public void conditionalReturnPasses() throws IOException {
        // iload_0; iconst_0; if_icmple +5; iconst_1; ireturn; iconst_0; ireturn
        // if taken: jump to iconst_0 at 7; if not taken: iconst_1 at 5, ireturn at 6
        // both paths end with ireturn (different ireturn instructions, no merge)
        byte[] code = {
            0x1a,                 // 0: iload_0
            0x03,                 // 1: iconst_0
            (byte) 0xa4, 0x00, 5, // 2: if_icmple +5 → 7
            0x04,                 // 5: iconst_1
            (byte) 0xac,          // 6: ireturn
            0x03,                 // 7: iconst_0
            (byte) 0xac           // 8: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "positive", "(I)I", code, 2, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "positive", "(I)I");
    }

    // --- invalid bytecode must throw VerifyException --------------------

    @Test(expected = VerifyException.class)
    public void wrongOperandTypeThrows() throws IOException {
        // iconst_0; aconst_null; iadd; ireturn  → iadd sees a reference where an int is expected
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "badAdd", "()I",
                    new byte[]{0x03, 0x01, 0x60, (byte) 0xac}, 2, 0);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "badAdd", "()I");
    }

    @Test
    public void mergingStackHeightsDifferThrows() throws IOException {
        // iload_0; iconst_0; if_icmpgt +7; iconst_0; goto +5; iload_0; iconst_1; ireturn
        // Path A (x <= 0): iconst_0 → goto merge → stack [0]
        // Path B (x > 0):  iload_0; iconst_1 → merge → stack [x, 1]
        // Merge at ireturn: depths 1 vs 2 → VerifyException
        byte[] code = {
            0x1a,                 // 0: iload_0
            0x03,                 // 1: iconst_0
            (byte) 0xa3, 0x00, 7, // 2: if_icmpgt +7 → 9
            0x03,                 // 5: iconst_0
            (byte) 0xa7, 0x00, 5, // 6: goto +5 → 11
            0x1a,                 // 9: iload_0
            0x04,                 // 10: iconst_1
            (byte) 0xac           // 11: ireturn
        };
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "badMerge", "(I)I", code, 2, 1);
        ClassData cd = VerifierTestUtil.load(b);
        try {
            VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "badMerge", "(I)I");
            fail("expected VerifyException for unequal stack heights at merge point");
        } catch (VerifyException e) {
            // The engine may report either the height mismatch or the
            // resulting ireturn failure — both are verification failures.
        }
    }

    @Test(expected = VerifyException.class)
    public void localVarTypeMismatchThrows() throws IOException {
        // aconst_null; astore_0; iload_0; ireturn  → iload reads a reference local as int
        ClassFileBuilder b = new ClassFileBuilder().classFile(C)
            .method(ClassFileBuilder.ACC_PUBLIC | ClassFileBuilder.ACC_STATIC,
                    "badLoad", "()I",
                    new byte[]{0x01, 0x3b, 0x1a, (byte) 0xac}, 1, 1);
        ClassData cd = VerifierTestUtil.load(b);
        VerifierTestUtil.typeCheck(VerifierTestUtil.finder(b), cd, "badLoad", "()I");
    }
}
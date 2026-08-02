package jx.verifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import jx.classfile.ClassData;
import jx.classfile.MethodData;
import jx.verifier.npa.NullPointerAnalysis;
import jx.verifier.typecheck.TypeCheck;

/** Shared plumbing for the jx.verifier tests. */
public final class VerifierTestUtil {

    private VerifierTestUtil() {
    }

    /** Parse a class file built by {@link ClassFileBuilder}. */
    public static ClassData load(ClassFileBuilder builder) throws IOException {
        byte[] bytes = builder.build();
        return new ClassData(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    /** Build and register several classes in a fresh finder. */
    public static TestClassFinder finder(ClassFileBuilder... builders) throws IOException {
        TestClassFinder finder = new TestClassFinder();
        for (ClassFileBuilder builder : builders) {
            byte[] bytes = builder.build();
            ClassData cd = new ClassData(new DataInputStream(new ByteArrayInputStream(bytes)));
            finder.register(cd.getClassName(), bytes);
        }
        return finder;
    }

    /** Run the type verifier on one method. */
    public static void typeCheck(TestClassFinder finder, ClassData cd,
                                 String methodName, String methodType) throws VerifyException {
        TypeCheck.init(finder);
        MethodData m = cd.getMethodData(methodName, methodType);
        TypeCheck.verifyMethod(m, cd.getClassName(), cd.getConstantPool());
    }

    /** Run the null-pointer analysis on one method. */
    public static void nullPointerAnalysis(TestClassFinder finder, ClassData cd,
                                           String methodName, String methodType) throws VerifyException {
        TypeCheck.init(finder);
        MethodData m = cd.getMethodData(methodName, methodType);
        NullPointerAnalysis.verifyMethod(m, cd.getClassName(), cd.getConstantPool());
    }
}

package jx.verifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import jx.verifier.typecheck.TCObjectTypes;
import jx.verifier.typecheck.TCTypes;
import jx.verifier.typecheck.TypeCheck;

/**
 * Tests for {@link TCObjectTypes} merging: two object types flowing into a
 * common control-flow point must be joined at their least common superclass.
 */
public class TCObjectTypesMergeTest {

    private static final String BASE = "jx/verifier/test/Base";
    private static final String MID = "jx/verifier/test/Mid";
    private static final String A = "jx/verifier/test/A";
    private static final String B = "jx/verifier/test/B";
    private static final String UNRELATED = "jx/verifier/test/Unrelated";

    private TestClassFinder finder;

    private void setUpHierarchy() throws IOException {
        ClassFileBuilder base = new ClassFileBuilder().classFile(BASE);
        ClassFileBuilder mid = new ClassFileBuilder().classFile(MID).superClass(BASE);
        ClassFileBuilder a = new ClassFileBuilder().classFile(A).superClass(MID);
        ClassFileBuilder b = new ClassFileBuilder().classFile(B).superClass(MID);
        ClassFileBuilder unrelated = new ClassFileBuilder().classFile(UNRELATED);
        finder = VerifierTestUtil.finder(base, mid, a, b, unrelated);
        TypeCheck.init(finder);
    }

    @Test
    public void siblingClassesMergeAtCommonSuperclass() throws IOException {
        setUpHierarchy();
        TCTypes merged = new TCObjectTypes(A).merge(new TCObjectTypes(B));
        assertTrue(merged instanceof TCObjectTypes);
        assertEquals(MID, ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void identicalClassesMergeToSameType() throws IOException {
        setUpHierarchy();
        TCTypes merged = new TCObjectTypes(A).merge(new TCObjectTypes(A));
        assertTrue(merged instanceof TCObjectTypes);
        assertEquals(A, ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void classAndSuperclassMergeToSuperclass() throws IOException {
        setUpHierarchy();
        TCTypes merged = new TCObjectTypes(A).merge(new TCObjectTypes(MID));
        assertEquals(MID, ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void unrelatedClassesMergeAtObject() throws IOException {
        setUpHierarchy();
        TCTypes merged = new TCObjectTypes(A).merge(new TCObjectTypes(UNRELATED));
        assertTrue(merged instanceof TCObjectTypes);
        assertEquals("java/lang/Object", ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void nullMergesIntoAnyObjectType() throws IOException {
        setUpHierarchy();
        TCTypes merged = TCTypes.T_NULL.merge(new TCObjectTypes(B));
        assertTrue(merged instanceof TCObjectTypes);
        assertEquals(B, ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void objectTypeAbsorbsNull() throws IOException {
        setUpHierarchy();
        TCTypes merged = new TCObjectTypes(B).merge(TCTypes.T_NULL);
        assertEquals(B, ((TCObjectTypes) merged).getClassName());
    }

    @Test
    public void findCommonSuperClassAcrossHierarchy() throws IOException {
        setUpHierarchy();
        String common = new TCObjectTypes(A).findCommonSuperClass(new TCObjectTypes(B));
        assertEquals(MID, common);
        common = new TCObjectTypes(A).findCommonSuperClass(new TCObjectTypes(UNRELATED));
        assertEquals("java/lang/Object", common);
    }

    @Test
    public void consistentWithDescendant() throws IOException {
        setUpHierarchy();
        // A is a descendant of Mid → consistent
        new TCObjectTypes(A).consistentWith(new TCObjectTypes(MID));
        // Mid is not a descendant of A → inconsistent
        boolean thrown = false;
        try {
            new TCObjectTypes(MID).consistentWith(new TCObjectTypes(A));
        } catch (VerifyException e) {
            thrown = true;
        }
        assertTrue("Mid.consistentWith(A) must fail", thrown);
        assertFalse(new TCObjectTypes(A).getClassName().isEmpty());
    }
}

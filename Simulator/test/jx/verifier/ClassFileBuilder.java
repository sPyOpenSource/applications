package jx.verifier;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal class-file assembler for verifier tests.
 * Builds standard-format class files (major version 52) from hand-written
 * bytecode, so tests can exercise the verifier with precisely controlled
 * instruction sequences — including ones javac would never emit.
 *
 * <p>Fluent usage:
 * <pre>
 * ClassFileBuilder b = new ClassFileBuilder()
 *     .classFile("jx/verifier/test/Calc")
 *     .method(ACC_PUBLIC | ACC_STATIC, "add", "(II)I",
 *             new byte[]{0x1a, 0x1b, 0x60, (byte)0xac}, 2, 2);
 * byte[] classFile = b.build();
 * </pre>
 */
public final class ClassFileBuilder {

    public static final int ACC_PUBLIC       = 0x0001;
    public static final int ACC_PRIVATE      = 0x0002;
    public static final int ACC_PROTECTED    = 0x0004;
    public static final int ACC_STATIC       = 0x0008;
    public static final int ACC_FINAL        = 0x0010;
    public static final int ACC_SUPER        = 0x0020;
    public static final int ACC_ABSTRACT     = 0x0400;

    private static final int TAG_UTF8        = 1;
    private static final int TAG_INTEGER     = 3;
    private static final int TAG_CLASS       = 7;
    private static final int TAG_FIELDREF    = 9;
    private static final int TAG_METHODREF   = 10;
    private static final int TAG_NAMEANDTYPE = 12;

    private final List<byte[]> entries = new ArrayList<>();
    private final Map<String, Integer> utf8Index = new HashMap<>();
    private final Map<String, Integer> classIndex = new HashMap<>();
    private final Map<String, Integer> natIndex = new HashMap<>();
    private final Map<String, Integer> methodRefIndex = new HashMap<>();
    private final Map<String, Integer> fieldRefIndex = new HashMap<>();

    private String thisClass = "jx/verifier/test/Fixture";
    private String superClass = "java/lang/Object";
    private int classAccess = ACC_PUBLIC | ACC_SUPER;
    private final List<String> interfaces = new ArrayList<>();

    private final List<Integer> fieldAccess = new ArrayList<>();
    private final List<String> fieldNames = new ArrayList<>();
    private final List<String> fieldDescs = new ArrayList<>();

    private final List<Integer> methodAccess = new ArrayList<>();
    private final List<String> methodNames = new ArrayList<>();
    private final List<String> methodDescs = new ArrayList<>();
    private final List<byte[]> methodCodes = new ArrayList<>();
    private final List<Integer> maxStacks = new ArrayList<>();
    private final List<Integer> maxLocals = new ArrayList<>();

    public ClassFileBuilder() {
        utf8("Code");
    }

    // ── fluent configuration ───────────────────────────────

    public ClassFileBuilder classFile(String internalName) {
        this.thisClass = internalName;
        return this;
    }

    public ClassFileBuilder superClass(String internalName) {
        this.superClass = internalName;
        return this;
    }

    public ClassFileBuilder access(int access) {
        this.classAccess = access | ACC_SUPER;
        return this;
    }

    public ClassFileBuilder interface_ (String internalName) {
        this.interfaces.add(internalName);
        return this;
    }

    public ClassFileBuilder field(int access, String name, String desc) {
        fieldAccess.add(access);
        fieldNames.add(name);
        fieldDescs.add(desc);
        utf8(name);
        utf8(desc);
        return this;
    }

    public ClassFileBuilder method(int access, String name, String desc,
                                   byte[] code, int maxStack, int maxLocals) {
        methodAccess.add(access);
        methodNames.add(name);
        methodDescs.add(desc);
        methodCodes.add(code.clone());
        maxStacks.add(maxStack);
        this.maxLocals.add(maxLocals);
        utf8(name);
        utf8(desc);
        return this;
    }

    // ── constant-pool accessors for building bytecode ──────

    public int cpUtf8(String s) { return utf8(s); }

    public int cpClass(String internalName) { return classEntry(internalName); }

    public int cpMethodRef(String owner, String name, String desc) { return methodRef(owner, name, desc); }

    public int cpFieldRef(String owner, String name, String desc) { return fieldRef(owner, name, desc); }

    // ── constant-pool entry writers ────────────────────────

    private int utf8(String s) {
        Integer i = utf8Index.get(s);
        if (i != null) return i;
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        byte[] e = new byte[3 + b.length];
        e[0] = TAG_UTF8;
        e[1] = (byte) (b.length >>> 8);
        e[2] = (byte) b.length;
        System.arraycopy(b, 0, e, 3, b.length);
        entries.add(e);
        int idx = entries.size();
        utf8Index.put(s, idx);
        return idx;
    }

    private int classEntry(String name) {
        Integer i = classIndex.get(name);
        if (i != null) return i;
        int ni = utf8(name);
        entries.add(new byte[]{TAG_CLASS, (byte) (ni >>> 8), (byte) ni});
        int idx = entries.size();
        classIndex.put(name, idx);
        return idx;
    }

    private int nameAndType(String name, String desc) {
        String key = name + " " + desc;
        Integer i = natIndex.get(key);
        if (i != null) return i;
        int n = utf8(name), d = utf8(desc);
        entries.add(new byte[]{TAG_NAMEANDTYPE,
                (byte) (n >>> 8), (byte) n,
                (byte) (d >>> 8), (byte) d});
        int idx = entries.size();
        natIndex.put(key, idx);
        return idx;
    }

    private int methodRef(String owner, String name, String desc) {
        String key = owner + " " + name + " " + desc;
        Integer i = methodRefIndex.get(key);
        if (i != null) return i;
        int c = classEntry(owner), nt = nameAndType(name, desc);
        entries.add(new byte[]{TAG_METHODREF,
                (byte) (c >>> 8), (byte) c,
                (byte) (nt >>> 8), (byte) nt});
        int idx = entries.size();
        methodRefIndex.put(key, idx);
        return idx;
    }

    private int fieldRef(String owner, String name, String desc) {
        String key = owner + " " + name + " " + desc;
        Integer i = fieldRefIndex.get(key);
        if (i != null) return i;
        int c = classEntry(owner), nt = nameAndType(name, desc);
        entries.add(new byte[]{TAG_FIELDREF,
                (byte) (c >>> 8), (byte) c,
                (byte) (nt >>> 8), (byte) nt});
        int idx = entries.size();
        fieldRefIndex.put(key, idx);
        return idx;
    }

    // ── class file emission ────────────────────────────────

    public byte[] build() {
        // Pre-resolve all class entries so they're in the constant pool before we write it
        classEntry(thisClass);
        classEntry(superClass);
        for (String ic : interfaces) classEntry(ic);
        for (String n : methodNames) { utf8(n); }
        for (String d : methodDescs) { utf8(d); }
        for (String n : fieldNames) { utf8(n); }
        for (String d : fieldDescs) { utf8(d); }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        try {
            out.writeInt(0xcafebabe);
            out.writeShort(0);
            out.writeShort(52);
            out.writeShort(entries.size() + 1);
            for (byte[] e : entries) out.write(e);

            out.writeShort(classAccess);
            out.writeShort(classEntry(thisClass));
            out.writeShort(classEntry(superClass));
            out.writeShort(interfaces.size());
            for (String ic : interfaces) out.writeShort(classEntry(ic));

            out.writeShort(fieldNames.size());
            for (int i = 0; i < fieldNames.size(); i++) {
                out.writeShort(fieldAccess.get(i));
                out.writeShort(utf8(fieldNames.get(i)));
                out.writeShort(utf8(fieldDescs.get(i)));
                out.writeShort(0);
            }

            out.writeShort(methodNames.size());
            for (int i = 0; i < methodNames.size(); i++) {
                out.writeShort(methodAccess.get(i));
                out.writeShort(utf8(methodNames.get(i)));
                out.writeShort(utf8(methodDescs.get(i)));
                out.writeShort(1);
                byte[] code = methodCodes.get(i);
                out.writeShort(utf8("Code"));
                out.writeInt(10 + code.length);
                out.writeShort(maxStacks.get(i));
                out.writeShort(maxLocals.get(i));
                out.writeInt(code.length);
                out.write(code);
                out.writeShort(0);
                out.writeShort(0);
            }

            out.writeShort(0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bos.toByteArray();
    }
}

package jCPU.JVM;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jx.classfile.ClassData;
import jx.classfile.CodeData;
import jx.classfile.MethodData;
import jx.classfile.constantpool.ClassCPEntry;
import jx.classfile.constantpool.ClassMemberCPEntry;
import jx.classfile.constantpool.ConstantPool;
import jx.classfile.constantpool.ConstantPoolEntry;
import jx.classfile.constantpool.NumericCPEntry;
import jx.classfile.constantpool.StringCPEntry;
import jx.classfile.datatypes.BCBasicDatatype;
import jx.classfile.datatypes.BCDouble;
import jx.classfile.datatypes.BCFloat;
import jx.classfile.datatypes.BCInteger;
import jx.classfile.datatypes.BCLong;

/**
 * A small, self-contained Java bytecode interpreter that lives in the
 * {@code jCPU.JVM} package.
 *
 * <p>The original {@code jCPU.JVM} classes ({@link CPU}, {@link BytecodeParser},
 * {@link BytecodeVisitor}) are a work in progress: they depend on the JNode
 * {@code jx.zero}/{@code jx.compiler} packages which are not part of this
 * source tree, and their execution state is never initialised.  This runner
 * keeps the same idea (a JVM implemented in the simulator tree) but uses the
 * class-file reader that ships with the repository ({@code jx.classfile}) so a
 * plain {@code .class} file can actually be executed.</p>
 *
 * <p>Usage:</p>
 *
 * <pre>
 *   java jCPU.JVM.JvmRunner HelloWorld.class
 *   java jCPU.JVM.JvmRunner HelloWorld arg1 arg2
 * </pre>
 *
 * <p>Execution models a stack machine over {@code Object}s: integers are
 * {@link Integer}s, longs are {@link Long}s, floats are {@link Float}s,
 * doubles are {@link Double}s, string constants are {@link String}s, object
 * instances are {@link Obj}, arrays are {@link ArrayRef} and
 * {@code System.out}/{@code System.err} are {@link PrintStreamRef}s.</p>
 *
 * @author jCPU.JVM
 */
public final class JvmRunner {

    /* ------------------------------------------------------------------ */
    /* VM state                                                           */
    /* ------------------------------------------------------------------ */

    private static final Map<String, ClassData> CLASS_CACHE = new LinkedHashMap<>();
    private static Path classPath = Paths.get(".");

    /** Static fields of interpreted classes, keyed by owner.name:desc. */
    private final Map<String, Object> staticFields = new HashMap<>();

    private static boolean verbose = false;

    /* ------------------------------------------------------------------ */
    /* Entry point                                                        */
    /* ------------------------------------------------------------------ */

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java jCPU.JVM.JvmRunner <class-file|class-name> [args...]");
            System.err.println("  e.g. java jCPU.JVM.JvmRunner HelloWorld.class");
            System.exit(2);
        }

        String target = args[0];
        String[] programArgs = Arrays.copyOfRange(args, 1, args.length);

        Path classFile;
        String binaryName;

        if (target.endsWith(".class") || Files.isRegularFile(Paths.get(target))) {
            classFile = Paths.get(target).toAbsolutePath();
            String fileName = classFile.getFileName().toString();
            binaryName = fileName.endsWith(".class")
                    ? fileName.substring(0, fileName.length() - ".class".length())
                    : fileName;
        } else {
            binaryName = target.replace('.', '/');
            classFile = null;
        }
        if (classFile == null) {
            classFile = Paths.get(binaryName + ".class").toAbsolutePath();
        }
        classPath = classFile.getParent();

        if (!Files.isRegularFile(classFile)) {
            System.err.println("class file not found: " + classFile);
            System.exit(1);
        }

        ClassData mainClass = loadClassFile(classFile, binaryName);
        MethodData mainMethod = mainClass.getMethodData("main", "([Ljava/lang/String;)V");
        if (mainMethod == null) {
            System.err.println("class " + binaryName + " has no main([Ljava/lang/String;)V");
            System.exit(1);
        }
        if (!mainMethod.isStatic()) {
            System.err.println("main([Ljava/lang/String;)V in " + binaryName + " is not static");
            System.exit(1);
        }

        JvmRunner vm = new JvmRunner();
        ArrayRef argv = new ArrayRef("java/lang/String", programArgs.length);
        System.arraycopy(programArgs, 0, argv.data, 0, programArgs.length);

        ClassData declaring = mainMethod.getDeclaringClass();
        vm.runMethod(declaring, mainMethod, null, new Object[]{argv});
    }

    /* ------------------------------------------------------------------ */
    /* Class + method loading                                             */
    /* ------------------------------------------------------------------ */

    private static ClassData loadClassFile(Path file, String binaryName) throws IOException {
        ClassData cached = CLASS_CACHE.get(binaryName);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = Files.readAllBytes(file);
        ClassData data = new ClassData(bytes);
        CLASS_CACHE.put(binaryName, data);
        return data;
    }

    /** Loads a class by its internal name (e.g. {@code java/lang/String}). */
    private static ClassData loadClassNamed(String internalName) throws IOException {
        ClassData cached = CLASS_CACHE.get(internalName);
        if (cached != null) {
            return cached;
        }
        Path file = classPath.resolve(internalName + ".class");
        if (!Files.isRegularFile(file)) {
            throw new IOException("cannot load class " + internalName + " (looked in " + file + ")");
        }
        return loadClassFile(file, internalName);
    }

    private static MethodData findMethod(ClassData cd, String name, String desc) {
        if (cd == null) {
            return null;
        }
        for (MethodData m : cd.getMethodData()) {
            if (m.getMethodName().equals(name) && m.getMethodType().equals(desc)) {
                return m;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Interpreter                                                        */
    /* ------------------------------------------------------------------ */

    private Object runMethod(ClassData owner, MethodData method, Object thisRef, Object[] arguments) {
        StackFrame frame = new StackFrame(owner, method, thisRef, arguments);
        if (verbose) {
            System.err.println("enter " + owner.getConstantPool().getClassName()
                    + "." + method.getMethodName() + method.getMethodType());
        }
        return execute(frame);
    }

    private Object execute(StackFrame f) {
        final byte[] code = f.bytecode;
        while (true) {
            int opPc = f.pc;
            int op = f.u1();

            switch (op) {
                /* ---- constants ---- */
                case 0x00: // nop
                    break;

                case 0x01: // aconst_null
                    f.push(null);
                    break;

                case 0x02: case 0x03: case 0x04: case 0x05:
                case 0x06: case 0x07: case 0x08: // iconst_<n>
                    f.push(op - 0x03);
                    break;

                case 0x09: case 0x0a: // lconst_0, lconst_1
                    f.push((long) (op - 0x09));
                    break;

                case 0x0b: case 0x0c: case 0x0d: // fconst_0..2
                    f.push((float) (op - 0x0b));
                    break;

                case 0x0e: case 0x0f: // dconst_0, dconst_1
                    f.push((double) (op - 0x0e));
                    break;

                case 0x10: // bipush
                    f.push((int) (byte) f.u1());
                    break;

                case 0x11: // sipush
                    f.push((int) (short) f.u2());
                    break;

                case 0x12: // ldc
                    ldc(f, f.u1(), false);
                    break;

                case 0x13: // ldc_w
                    ldc(f, f.u2(), false);
                    break;

                case 0x14: // ldc2_w
                    ldc(f, f.u2(), true);
                    break;

                /* ---- loads ---- */
                case 0x15: f.push(f.local(f.u1())); break;
                case 0x16: f.push(f.local(f.u1())); break;
                case 0x17: f.push(f.local(f.u1())); break;
                case 0x18: f.push(f.local(f.u1())); break;
                case 0x19: f.push(f.local(f.u1())); break;

                case 0x1a: case 0x1b: case 0x1c: case 0x1d: // iload_0..3
                    f.push(f.local(op - 0x1a));
                    break;
                case 0x1e: case 0x1f: case 0x20: case 0x21: // lload_0..3
                    f.push(f.local(op - 0x1e));
                    break;
                case 0x22: case 0x23: case 0x24: case 0x25: // fload_0..3
                    f.push(f.local(op - 0x22));
                    break;
                case 0x26: case 0x27: case 0x28: case 0x29: // dload_0..3
                    f.push(f.local(op - 0x26));
                    break;
                case 0x2a: case 0x2b: case 0x2c: case 0x2d: // aload_0..3
                    f.push(f.local(op - 0x2a));
                    break;

                /* ---- array loads ---- */
                case 0x2e: case 0x2f: case 0x30: case 0x31:
                case 0x32: case 0x33: case 0x34: case 0x35:
                    arrayLoad(f);
                    break;

                /* ---- stores ---- */
                case 0x36: case 0x37: case 0x38: case 0x39: case 0x3a:
                    f.local(f.u1(), f.pop());
                    break;

                case 0x3b: case 0x3c: case 0x3d: case 0x3e: // istore_0..3
                    f.local(op - 0x3b, f.pop());
                    break;
                case 0x3f: case 0x40: case 0x41: case 0x42: // lstore_0..3
                    f.local(op - 0x3f, f.pop());
                    break;
                case 0x43: case 0x44: case 0x45: case 0x46: // fstore_0..3
                    f.local(op - 0x43, f.pop());
                    break;
                case 0x47: case 0x48: case 0x49: case 0x4a: // dstore_0..3
                    f.local(op - 0x47, f.pop());
                    break;
                case 0x4b: case 0x4c: case 0x4d: case 0x4e: // astore_0..3
                    f.local(op - 0x4b, f.pop());
                    break;

                /* ---- array stores ---- */
                case 0x4f: case 0x50: case 0x51: case 0x52:
                case 0x53: case 0x54: case 0x55: case 0x56:
                    arrayStore(f);
                    break;

                /* ---- stack manipulation ---- */
                case 0x57: // pop
                    f.pop();
                    break;
                case 0x58: // pop2
                    if (isTwoSlot(f.peek())) { f.pop(); } else { f.pop(); f.pop(); }
                    break;
                case 0x59: // dup
                    f.push(f.peek());
                    break;
                case 0x5a: { // dup_x1
                    Object v1 = f.pop();
                    Object v2 = f.pop();
                    f.push(v1); f.push(v2); f.push(v1);
                    break;
                }
                case 0x5b: { // dup_x2
                    Object v1 = f.pop();
                    Object v2 = f.pop();
                    Object v3 = f.pop();
                    f.push(v1); f.push(v3); f.push(v2); f.push(v1);
                    break;
                }
                case 0x5c: { // dup2
                    Object v1 = f.pop();
                    if (isTwoSlot(v1)) {
                        f.push(v1); f.push(v1);
                    } else {
                        Object v2 = f.pop();
                        f.push(v2); f.push(v1); f.push(v2); f.push(v1);
                    }
                    break;
                }
                case 0x5d: { // dup2_x1
                    Object v1 = f.pop();
                    Object v2 = f.pop();
                    Object v3 = f.pop();
                    f.push(v1); f.push(v2); f.push(v3); f.push(v1); f.push(v2);
                    break;
                }
                case 0x5e: { // dup2_x2
                    Object v1 = f.pop();
                    Object v2 = f.pop();
                    Object v3 = f.pop();
                    Object v4 = f.pop();
                    f.push(v1); f.push(v2); f.push(v3); f.push(v4);
                    f.push(v1); f.push(v2);
                    break;
                }
                case 0x5f: { // swap
                    Object v1 = f.pop();
                    Object v2 = f.pop();
                    f.push(v1); f.push(v2);
                    break;
                }

                /* ---- integer arithmetic ---- */
                case 0x60: f.push(f.popInt() + f.popInt()); break;
                case 0x64: { int b = f.popInt(); int a = f.popInt(); f.push(a - b); break; }
                case 0x68: f.push(f.popInt() * f.popInt()); break;
                case 0x6c: { int b = f.popInt(); int a = f.popInt(); f.push(a / b); break; }
                case 0x70: { int b = f.popInt(); int a = f.popInt(); f.push(a % b); break; }
                case 0x74: f.push(-f.popInt()); break;
                case 0x78: { int b = f.popInt(); int a = f.popInt(); f.push(a << b); break; }
                case 0x7a: { int b = f.popInt(); int a = f.popInt(); f.push(a >> b); break; }
                case 0x7c: { int b = f.popInt(); int a = f.popInt(); f.push(a >>> b); break; }
                case 0x7e: f.push(f.popInt() & f.popInt()); break;
                case 0x80: f.push(f.popInt() | f.popInt()); break;
                case 0x82: f.push(f.popInt() ^ f.popInt()); break;
                case 0x84: { // iinc
                    int index = f.u1();
                    int amount = (byte) f.u1();
                    f.local(index, f.intLocal(index) + amount);
                    break;
                }

                /* ---- long arithmetic ---- */
                case 0x61: { long b = f.popLong(); long a = f.popLong(); f.push(a + b); break; }
                case 0x65: { long b = f.popLong(); long a = f.popLong(); f.push(a - b); break; }
                case 0x69: { long b = f.popLong(); long a = f.popLong(); f.push(a * b); break; }
                case 0x6d: { long b = f.popLong(); long a = f.popLong(); f.push(a / b); break; }
                case 0x71: { long b = f.popLong(); long a = f.popLong(); f.push(a % b); break; }
                case 0x75: f.push(-f.popLong()); break;
                case 0x79: { int b = f.popInt(); long a = f.popLong(); f.push(a << b); break; }
                case 0x7b: { int b = f.popInt(); long a = f.popLong(); f.push(a >> b); break; }
                case 0x7d: { int b = f.popInt(); long a = f.popLong(); f.push(a >>> b); break; }
                case 0x7f: { long b = f.popLong(); long a = f.popLong(); f.push(a & b); break; }
                case 0x81: { long b = f.popLong(); long a = f.popLong(); f.push(a | b); break; }
                case 0x83: { long b = f.popLong(); long a = f.popLong(); f.push(a ^ b); break; }

                /* ---- float arithmetic ---- */
                case 0x62: { float b = f.popFloat(); float a = f.popFloat(); f.push(a + b); break; }
                case 0x66: { float b = f.popFloat(); float a = f.popFloat(); f.push(a - b); break; }
                case 0x6a: { float b = f.popFloat(); float a = f.popFloat(); f.push(a * b); break; }
                case 0x6e: { float b = f.popFloat(); float a = f.popFloat(); f.push(a / b); break; }
                case 0x72: { float b = f.popFloat(); float a = f.popFloat(); f.push(a % b); break; }
                case 0x76: f.push(-f.popFloat()); break;

                /* ---- double arithmetic ---- */
                case 0x63: { double b = f.popDouble(); double a = f.popDouble(); f.push(a + b); break; }
                case 0x67: { double b = f.popDouble(); double a = f.popDouble(); f.push(a - b); break; }
                case 0x6b: { double b = f.popDouble(); double a = f.popDouble(); f.push(a * b); break; }
                case 0x6f: { double b = f.popDouble(); double a = f.popDouble(); f.push(a / b); break; }
                case 0x73: { double b = f.popDouble(); double a = f.popDouble(); f.push(a % b); break; }
                case 0x77: f.push(-f.popDouble()); break;

                /* ---- conversions ---- */
                case 0x85: f.push((long) f.popInt()); break;      // i2l
                case 0x86: f.push((float) f.popInt()); break;     // i2f
                case 0x87: f.push((double) f.popInt()); break;    // i2d
                case 0x88: f.push((int) f.popLong()); break;      // l2i
                case 0x89: f.push((float) f.popLong()); break;    // l2f
                case 0x8a: f.push((double) f.popLong()); break;   // l2d
                case 0x8b: f.push((int) f.popFloat()); break;     // f2i
                case 0x8c: f.push((long) f.popFloat()); break;    // f2l
                case 0x8d: f.push((double) f.popFloat()); break;  // f2d
                case 0x8e: f.push((int) f.popDouble()); break;    // d2i
                case 0x8f: f.push((long) f.popDouble()); break;   // d2l
                case 0x90: f.push((float) f.popDouble()); break;  // d2f
                case 0x91: f.push((int) (byte) f.popInt()); break;  // i2b
                case 0x92: f.push((int) (char) f.popInt()); break;  // i2c
                case 0x93: f.push((int) (short) f.popInt()); break; // i2s

                /* ---- comparisons ---- */
                case 0x94: { long b = f.popLong(); long a = f.popLong(); f.push(Long.compare(a, b)); break; }
                case 0x95: { float b = f.popFloat(); float a = f.popFloat(); f.push(fcmp(a, b, -1)); break; }
                case 0x96: { float b = f.popFloat(); float a = f.popFloat(); f.push(fcmp(a, b, 1)); break; }
                case 0x97: { double b = f.popDouble(); double a = f.popDouble(); f.push(fcmp(a, b, -1)); break; }
                case 0x98: { double b = f.popDouble(); double a = f.popDouble(); f.push(fcmp(a, b, 1)); break; }

                /* ---- conditional branches ---- */
                case 0x99: if (f.popInt() == 0) f.pc = opPc + f.s2(); else f.skip2(); break; // ifeq
                case 0x9a: if (f.popInt() != 0) f.pc = opPc + f.s2(); else f.skip2(); break; // ifne
                case 0x9b: if (f.popInt() < 0) f.pc = opPc + f.s2(); else f.skip2(); break;
                case 0x9c: if (f.popInt() >= 0) f.pc = opPc + f.s2(); else f.skip2(); break;
                case 0x9d: if (f.popInt() > 0) f.pc = opPc + f.s2(); else f.skip2(); break;
                case 0x9e: if (f.popInt() <= 0) f.pc = opPc + f.s2(); else f.skip2(); break;

                case 0x9f: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a == b); break; }
                case 0xa0: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a != b); break; }
                case 0xa1: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a < b); break; }
                case 0xa2: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a >= b); break; }
                case 0xa3: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a > b); break; }
                case 0xa4: { int b = f.popInt(); int a = f.popInt(); branch(f, opPc, a <= b); break; }

                case 0xa5: { Object b = f.pop(); Object a = f.pop(); branch(f, opPc, a == b); break; }
                case 0xa6: { Object b = f.pop(); Object a = f.pop(); branch(f, opPc, a != b); break; }

                case 0xa7: // goto
                    f.pc = opPc + f.s2();
                    break;

                case 0xaa: // tableswitch
                    f.pc += (4 - (f.pc % 4)) % 4;
                    {
                        int def = f.s4();
                        int low = f.s4();
                        int high = f.s4();
                        int index = f.popInt();
                        if (index < low || index > high) {
                            f.pc = opPc + def;
                        } else {
                            int offset = f.s4();
                            for (int i = low; i < index; i++) {
                                f.skip4();
                            }
                            f.pc = opPc + offset;
                        }
                    }
                    break;

                case 0xab: // lookupswitch
                    f.pc += (4 - (f.pc % 4)) % 4;
                    {
                        int def = f.s4();
                        int nPairs = f.s4();
                        int key = f.popInt();
                        int chosen = def;
                        for (int i = 0; i < nPairs; i++) {
                            int match = f.s4();
                            int offset = f.s4();
                            if (match == key) {
                                chosen = offset;
                            }
                        }
                        f.pc = opPc + chosen;
                    }
                    break;

                /* ---- returns ---- */
                case 0xac: return f.pop();                                  // ireturn
                case 0xad: return f.pop();                                  // lreturn
                case 0xae: return f.pop();                                  // freturn
                case 0xaf: return f.pop();                                  // dreturn
                case 0xb0: return f.pop();                                  // areturn
                case 0xb1: return null;                                     // return

                /* ---- fields ---- */
                case 0xb2: { // getstatic
                    ClassMemberCPEntry e = member(f.cp, f.u2());
                    f.push(getStatic(e.getClassName(), e.getMemberName(), e.getMemberTypeDesc()));
                    break;
                }
                case 0xb3: { // putstatic
                    ClassMemberCPEntry e = member(f.cp, f.u2());
                    putStatic(e.getClassName(), e.getMemberName(), e.getMemberTypeDesc(), f.pop());
                    break;
                }
                case 0xb4: { // getfield
                    ClassMemberCPEntry e = member(f.cp, f.u2());
                    Object target = f.pop();
                    f.push(getField(target, e.getMemberName(), e.getMemberTypeDesc()));
                    break;
                }
                case 0xb5: { // putfield
                    ClassMemberCPEntry e = member(f.cp, f.u2());
                    Object value = f.pop();
                    Object target = f.pop();
                    putField(target, e.getMemberName(), value);
                    break;
                }

                /* ---- method invocation ---- */
                case 0xb6: invoke(f, f.u2(), false, false); break; // invokevirtual
                case 0xb7: invoke(f, f.u2(), false, true); break;  // invokespecial
                case 0xb8: invoke(f, f.u2(), true, false); break;  // invokestatic
                case 0xb9: { invoke(f, f.u2(), false, false); f.u1(); f.u1(); break; } // invokeinterface

                case 0xba: // invokedynamic
                    throw new VMException("invokedynamic is not supported (pc=" + opPc + ")");

                /* ---- objects and arrays ---- */
                case 0xbb: { // new
                    ClassCPEntry e = f.cp.classEntryAt(f.u2());
                    String className = e.getClassName();
                    if ("java/lang/StringBuilder".equals(className)
                            || "java/lang/StringBuffer".equals(className)) {
                        f.push(new StringBuilderRef());
                    } else {
                        f.push(new Obj(className));
                    }
                    break;
                }
                case 0xbc: { // newarray
                    int atype = f.u1();
                    int count = f.popInt();
                    f.push(newArray(componentForAtype(atype), count));
                    break;
                }
                case 0xbd: { // anewarray
                    ClassCPEntry e = f.cp.classEntryAt(f.u2());
                    int count = f.popInt();
                    f.push(newArray(e.getClassName(), count));
                    break;
                }
                case 0xbe: { // arraylength
                    ArrayRef a = (ArrayRef) f.pop();
                    f.push(a.data.length);
                    break;
                }
                case 0xbf: { // athrow
                    Object ex = f.pop();
                    throw new VMException("exception thrown: " + valueToString(ex));
                }
                case 0xc0: // checkcast -- no runtime type check needed here
                    break;
                case 0xc1: { // instanceof
                    Object v = f.pop();
                    ClassCPEntry e = f.cp.classEntryAt(f.u2());
                    f.push(instanceOf(v, e.getClassName()) ? 1 : 0);
                    break;
                }
                case 0xc2: case 0xc3: // monitorenter / monitorexit
                    f.pop();
                    break;
                case 0xc4: { // wide
                    int wideOp = f.u1();
                    int index = f.u2();
                    switch (wideOp) {
                        case 0x15: case 0x16: case 0x17: case 0x18: case 0x19:
                            f.push(f.local(index));
                            break;
                        case 0x36: case 0x37: case 0x38: case 0x39: case 0x3a:
                            f.local(index, f.pop());
                            break;
                        case 0x84:
                            int amount = f.s2();
                            f.local(index, f.intLocal(index) + amount);
                            break;
                        default:
                            throw new VMException("unsupported wide opcode 0x"
                                    + Integer.toHexString(wideOp) + " (pc=" + opPc + ")");
                    }
                    break;
                }
                case 0xc5: { // multianewarray
                    ClassCPEntry e = f.cp.classEntryAt(f.u2());
                    int dims = f.u1();
                    int[] counts = new int[dims];
                    for (int i = dims - 1; i >= 0; i--) {
                        counts[i] = f.popInt();
                    }
                    f.push(newMultiArray(e.getClassName(), counts, 0));
                    break;
                }
                case 0xc6: // ifnull
                    if (f.pop() == null) f.pc = opPc + f.s2(); else f.skip2();
                    break;
                case 0xc7: // ifnonnull
                    if (f.pop() != null) f.pc = opPc + f.s2(); else f.skip2();
                    break;
                case 0xc8: // goto_w
                    f.pc = opPc + f.s4();
                    break;

                default:
                    throw new VMException(String.format(
                            "unsupported opcode 0x%02x at pc=%d in %s.%s%s",
                            op, opPc,
                            f.owner == null ? "?" : f.owner.getConstantPool().getClassName(),
                            f.method.getMethodName(), f.method.getMethodType()));
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Constant pool helpers                                              */
    /* ------------------------------------------------------------------ */

    private static void ldc(StackFrame f, int index, boolean twoSlot) {
        ConstantPoolEntry entry = f.cp.entryAt(index);
        if (entry instanceof StringCPEntry) {
            f.push(((StringCPEntry) entry).value());
        } else if (entry instanceof NumericCPEntry) {
            BCBasicDatatype v = ((NumericCPEntry) entry).value();
            if (v instanceof BCInteger) {
                f.push(((BCInteger) v).value());
            } else if (v instanceof BCLong) {
                f.push(((BCLong) v).value());
            } else if (v instanceof BCFloat) {
                f.push(((BCFloat) v).value());
            } else if (v instanceof BCDouble) {
                f.push(((BCDouble) v).value());
            } else {
                throw new VMException("unsupported numeric constant " + v);
            }
        } else if (entry instanceof ClassCPEntry) {
            f.push(((ClassCPEntry) entry).getClassName());
        } else {
            throw new VMException("unsupported ldc entry " + entry);
        }
    }

    private static ClassMemberCPEntry member(ConstantPool cp, int index) {
        ConstantPoolEntry entry = cp.entryAt(index);
        if (!(entry instanceof ClassMemberCPEntry)) {
            throw new VMException("constant pool entry " + index + " is not a member reference: " + entry);
        }
        return (ClassMemberCPEntry) entry;
    }

    /* ------------------------------------------------------------------ */
    /* Field access                                                       */
    /* ------------------------------------------------------------------ */

    private Object getStatic(String owner, String name, String desc) {
        if ("java/lang/System".equals(owner)) {
            switch (name) {
                case "out": return new PrintStreamRef(System.out);
                case "err": return new PrintStreamRef(System.err);
                case "in": return null;
                default: break;
            }
        }
        String key = owner + "." + name + ":" + desc;
        if (staticFields.containsKey(key)) {
            return staticFields.get(key);
        }
        return defaultValue(desc);
    }

    private void putStatic(String owner, String name, String desc, Object value) {
        staticFields.put(owner + "." + name + ":" + desc, value);
    }

    private static Object getField(Object target, String name, String desc) {
        if (target instanceof Obj) {
            Object v = ((Obj) target).fields.get(name);
            return v != null ? v : defaultValue(desc);
        }
        throw new VMException("getfield on non-object " + valueToString(target) + "." + name);
    }

    private static void putField(Object target, String name, Object value) {
        if (!(target instanceof Obj)) {
            throw new VMException("putfield on non-object " + valueToString(target));
        }
        ((Obj) target).fields.put(name, value);
    }

    private static Object defaultValue(String desc) {
        switch (desc.charAt(0)) {
            case 'Z': case 'B': case 'C': case 'S': case 'I':
                return 0;
            case 'J': return 0L;
            case 'F': return 0f;
            case 'D': return 0d;
            default: return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Method invocation                                                  */
    /* ------------------------------------------------------------------ */

    private void invoke(StackFrame f, int cpIndex, boolean isStatic, boolean isSpecial) {
        ClassMemberCPEntry e = member(f.cp, cpIndex);
        String owner = e.getClassName();
        String name = e.getMemberName();
        String desc = e.getMemberTypeDesc();

        List<String> types = paramTypes(desc);
        Object[] args = new Object[types.size()];
        for (int i = types.size() - 1; i >= 0; i--) {
            args[i] = popOfType(f, types.get(i));
        }

        Object target = null;
        if (!isStatic) {
            target = f.pop();
        }

        Object result = invokeResolved(owner, name, desc, target, args, isStatic);
        if (!"V".equals(returnType(desc))) {
            f.push(result);
        }
    }

    private Object invokeResolved(String owner, String name, String desc,
                                  Object target, Object[] args, boolean isStatic) {
        /* --- built-in runtime classes ---------------------------------- */
        if (owner.startsWith("java/") || owner.startsWith("javax/")) {
            Object result = invokeBuiltin(owner, name, desc, target, args);
            if (result != NOT_HANDLED) {
                return result;
            }
            if (!isStatic && target instanceof Obj) {
                // fall through to interpreted user class below
            } else {
                throw new VMException("unsupported runtime method " + owner + "." + name + desc);
            }
        }

        /* --- interpreted classes --------------------------------------- */
        ClassData cd = null;
        String className = owner;
        if (target instanceof Obj) {
            className = ((Obj) target).className;
        }
        try {
            cd = loadClassNamed(className);
        } catch (IOException ex) {
            throw new VMException("cannot load class " + className + ": " + ex.getMessage());
        }
        MethodData method = findMethod(cd, name, desc);
        if (method == null) {
            if ("<init>".equals(name) && "java/lang/Object".equals(owner)) {
                return null;
            }
            if ("toString".equals(name) && "()Ljava/lang/String;".equals(desc)) {
                return (target instanceof Obj ? ((Obj) target).className : "?") + "@"
                        + Integer.toHexString(System.identityHashCode(target));
            }
            if ("equals".equals(name) && "(Ljava/lang/Object;)Z".equals(desc)) {
                return target == args[0] ? 1 : 0;
            }
            if ("hashCode".equals(name) && "()I".equals(desc)) {
                return System.identityHashCode(target);
            }
            throw new VMException("cannot find method " + className + "." + name + desc);
        }
        return runMethod(cd, method, isStatic ? null : target, args);
    }

    private static final Object NOT_HANDLED = new Object();
    private static final Object VOID = new Object();

    private Object invokeBuiltin(String owner, String name, String desc,
                                 Object target, Object[] args) {
        switch (owner) {
            case "java/lang/Object":
                if ("<init>".equals(name)) {
                    return VOID;
                }
                if ("toString".equals(name)) {
                    return target == null ? "null" : valueToString(target);
                }
                if ("equals".equals(name)) {
                    return target == args[0] ? 1 : 0;
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(target);
                }
                return NOT_HANDLED;

            case "java/io/PrintStream":
                if (target instanceof PrintStreamRef) {
                    return printStream((PrintStreamRef) target, name, desc, args);
                }
                return NOT_HANDLED;

            case "java/lang/StringBuilder":
                if (target instanceof StringBuilderRef) {
                    return stringBuilder(name, desc, (StringBuilderRef) target, args);
                }
                return NOT_HANDLED;

            case "java/lang/String":
                return stringMethod(name, desc, target, args);

            case "java/lang/System":
                return systemMethod(name, desc, args);

            case "java/lang/Math":
                return mathMethod(name, desc, args);

            case "java/lang/Integer":
                return integerMethod(name, desc, target, args);

            case "java/lang/Long":
                if ("toString".equals(name)) return String.valueOf(((Number) args[0]).longValue());
                if ("valueOf".equals(name)) return ((Number) args[0]).longValue();
                if ("parseLong".equals(name)) return Long.parseLong((String) args[0]);
                if ("longValue".equals(name)) return ((Number) target).longValue();
                return NOT_HANDLED;

            case "java/lang/Double":
                if ("toString".equals(name)) return String.valueOf(((Number) args[0]).doubleValue());
                if ("valueOf".equals(name)) return ((Number) args[0]).doubleValue();
                if ("parseDouble".equals(name)) return Double.parseDouble((String) args[0]);
                if ("doubleValue".equals(name)) return ((Number) target).doubleValue();
                return NOT_HANDLED;

            case "java/lang/Boolean":
                if ("toString".equals(name)) return String.valueOf(((Number) args[0]).intValue() != 0);
                if ("valueOf".equals(name)) return ((Number) args[0]).intValue();
                if ("booleanValue".equals(name)) return ((Number) target).intValue();
                return NOT_HANDLED;

            case "java/lang/Character":
                if ("toString".equals(name)) return String.valueOf((char) ((Number) args[0]).intValue());
                if ("isDigit".equals(name)) return Character.isDigit((char) ((Number) args[0]).intValue()) ? 1 : 0;
                if ("isLetter".equals(name)) return Character.isLetter((char) ((Number) args[0]).intValue()) ? 1 : 0;
                if ("charValue".equals(name)) return ((Number) target).intValue();
                return NOT_HANDLED;

            case "java/lang/Enum":
                if ("name".equals(name)) return ((Obj) target).fields.get("name");
                if ("ordinal".equals(name)) return ((Obj) target).fields.get("ordinal");
                return NOT_HANDLED;

            default:
                return NOT_HANDLED;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Built-in implementations                                           */
    /* ------------------------------------------------------------------ */

    private static Object printStream(PrintStreamRef ref, String name, String desc, Object[] args) {
        switch (name) {
            case "print":
                if (args.length == 0) {
                    // nothing
                } else if (args[0] instanceof ArrayRef && isCharArray((ArrayRef) args[0])) {
                    ref.out.print(charsToString((ArrayRef) args[0]));
                } else {
                    ref.out.print(valueToString(args[0]));
                }
                break;
            case "println":
                if (args.length == 0) {
                    ref.out.println();
                } else if (args[0] instanceof ArrayRef && isCharArray((ArrayRef) args[0])) {
                    ref.out.println(charsToString((ArrayRef) args[0]));
                } else {
                    ref.out.println(valueToString(args[0]));
                }
                break;
            case "printf":
            case "format":
                String fmt = String.valueOf(args[0]);
                Object[] fa = args.length > 1 && args[1] instanceof ArrayRef
                        ? ((ArrayRef) args[1]).data
                        : new Object[0];
                ref.out.printf(fmt, fa);
                break;
            case "flush":
                ref.out.flush();
                break;
            case "write":
                if (args.length == 1) {
                    ref.out.write(((Number) args[0]).intValue());
                } else {
                    ArrayRef bytes = (ArrayRef) args[0];
                    int off = ((Number) args[1]).intValue();
                    int len = ((Number) args[2]).intValue();
                    byte[] tmp = new byte[len];
                    for (int i = 0; i < len; i++) {
                        tmp[i] = (byte) ((Number) bytes.data[off + i]).intValue();
                    }
                    ref.out.write(tmp, 0, len);
                }
                break;
            default:
                throw new VMException("unsupported PrintStream method " + name + desc);
        }
        return "V".equals(returnType(desc)) ? VOID : ref;
    }

    private Object stringBuilder(String name, String desc, StringBuilderRef target, Object[] args) {
        if ("<init>".equals(name)) {
            if (args.length == 1 && args[0] != null) {
                if (args[0] instanceof Integer) {
                    target.sb = new StringBuilder(((Integer) args[0]).intValue());
                } else {
                    target.sb = new StringBuilder(valueToString(args[0]));
                }
            }
            return VOID;
        }
        switch (name) {
            case "append":
                if (args[0] instanceof ArrayRef && isCharArray((ArrayRef) args[0])) {
                    target.sb.append(charsToString((ArrayRef) args[0]));
                } else {
                    target.sb.append(valueToString(args[0]));
                }
                return target;
            case "toString":
                return target.sb.toString();
            case "length":
                return target.sb.length();
            case "charAt":
                return (int) target.sb.charAt(((Number) args[0]).intValue());
            case "reverse":
                target.sb.reverse();
                return target;
            case "insert":
                target.sb.insert(((Number) args[0]).intValue(), args[1]);
                return target;
            case "deleteCharAt":
                target.sb.deleteCharAt(((Number) args[0]).intValue());
                return target;
            case "setLength":
                target.sb.setLength(((Number) args[0]).intValue());
                return target;
            default:
                throw new VMException("unsupported StringBuilder method " + name + desc);
        }
    }

    private Object stringMethod(String name, String desc, Object target, Object[] args) {
        if ("valueOf".equals(name)) {
            return args[0] == null ? "null" : valueToString(args[0]);
        }
        if ("format".equals(name)) {
            Object[] fa = args.length > 1 && args[1] instanceof ArrayRef
                    ? ((ArrayRef) args[1]).data
                    : new Object[0];
            return String.format(String.valueOf(args[0]), fa);
        }
        if (target == null) {
            throw new VMException("String method " + name + " called on null");
        }
        String s = (String) target;
        switch (name) {
            case "length": return s.length();
            case "isEmpty": return s.isEmpty() ? 1 : 0;
            case "charAt": return (int) s.charAt(((Number) args[0]).intValue());
            case "equals": return s.equals(args[0]) ? 1 : 0;
            case "equalsIgnoreCase": return s.equalsIgnoreCase(valueToString(args[0])) ? 1 : 0;
            case "hashCode": return s.hashCode();
            case "toString": return s;
            case "indexOf": return s.indexOf(valueToString(args[0]));
            case "lastIndexOf": return s.lastIndexOf(valueToString(args[0]));
            case "contains": return s.contains(valueToString(args[0])) ? 1 : 0;
            case "startsWith": return s.startsWith(valueToString(args[0])) ? 1 : 0;
            case "endsWith": return s.endsWith(valueToString(args[0])) ? 1 : 0;
            case "substring":
                if (args.length == 1) return s.substring(((Number) args[0]).intValue());
                return s.substring(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
            case "concat": return s.concat(valueToString(args[0]));
            case "toUpperCase": return s.toUpperCase();
            case "toLowerCase": return s.toLowerCase();
            case "trim": return s.trim();
            case "replace":
                return s.replace(valueToString(args[0]), valueToString(args[1]));
            case "toCharArray": {
                char[] chars = s.toCharArray();
                ArrayRef a = new ArrayRef("char", chars.length);
                for (int i = 0; i < chars.length; i++) a.data[i] = (int) chars[i];
                return a;
            }
            case "split": {
                String[] parts = s.split(valueToString(args[0]));
                ArrayRef a = new ArrayRef("java/lang/String", parts.length);
                a.data = parts;
                return a;
            }
            case "compareTo": return s.compareTo(valueToString(args[0]));
            default:
                throw new VMException("unsupported String method " + name + desc);
        }
    }

    private Object systemMethod(String name, String desc, Object[] args) {
        switch (name) {
            case "currentTimeMillis": return System.currentTimeMillis();
            case "nanoTime": return System.nanoTime();
            case "identityHashCode": return System.identityHashCode(args[0]);
            case "arraycopy": {
                ArrayRef src = (ArrayRef) args[0];
                int srcPos = ((Number) args[1]).intValue();
                ArrayRef dst = (ArrayRef) args[2];
                int dstPos = ((Number) args[3]).intValue();
                int len = ((Number) args[4]).intValue();
                System.arraycopy(src.data, srcPos, dst.data, dstPos, len);
                return VOID;
            }
            case "exit": {
                System.out.flush();
                System.exit(((Number) args[0]).intValue());
                return VOID;
            }
            case "getProperty":
            case "getenv":
                return null;
            default:
                throw new VMException("unsupported System method " + name + desc);
        }
    }

    private Object mathMethod(String name, String desc, Object[] args) {
        switch (name) {
            case "abs":
                if (args[0] instanceof Integer) return Math.abs(((Integer) args[0]));
                if (args[0] instanceof Long) return Math.abs(((Long) args[0]));
                if (args[0] instanceof Float) return Math.abs(((Float) args[0]));
                return Math.abs(((Double) args[0]));
            case "max":
                if (args[0] instanceof Integer) return Math.max(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
                if (args[0] instanceof Long) return Math.max(((Number) args[0]).longValue(), ((Number) args[1]).longValue());
                if (args[0] instanceof Float) return Math.max(((Number) args[0]).floatValue(), ((Number) args[1]).floatValue());
                return Math.max(((Number) args[0]).doubleValue(), ((Number) args[1]).doubleValue());
            case "min":
                if (args[0] instanceof Integer) return Math.min(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
                if (args[0] instanceof Long) return Math.min(((Number) args[0]).longValue(), ((Number) args[1]).longValue());
                if (args[0] instanceof Float) return Math.min(((Number) args[0]).floatValue(), ((Number) args[1]).floatValue());
                return Math.min(((Number) args[0]).doubleValue(), ((Number) args[1]).doubleValue());
            case "sqrt": return Math.sqrt(((Number) args[0]).doubleValue());
            case "pow": return Math.pow(((Number) args[0]).doubleValue(), ((Number) args[1]).doubleValue());
            case "floor": return Math.floor(((Number) args[0]).doubleValue());
            case "ceil": return Math.ceil(((Number) args[0]).doubleValue());
            case "round": return Math.round(((Number) args[0]).doubleValue());
            case "random": return Math.random();
            default:
                throw new VMException("unsupported Math method " + name + desc);
        }
    }

    private Object integerMethod(String name, String desc, Object target, Object[] args) {
        switch (name) {
            case "toString": return String.valueOf(((Number) args[0]).intValue());
            case "toHexString": return Integer.toHexString(((Number) args[0]).intValue());
            case "toBinaryString": return Integer.toBinaryString(((Number) args[0]).intValue());
            case "parseInt": return Integer.parseInt((String) args[0]);
            case "valueOf": return ((Number) args[0]).intValue();
            case "intValue": return ((Number) target).intValue();
            case "compare": return Integer.compare(((Number) args[0]).intValue(), ((Number) args[1]).intValue());
            default:
                throw new VMException("unsupported Integer method " + name + desc);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Arrays                                                             */
    /* ------------------------------------------------------------------ */

    private static void arrayLoad(StackFrame f) {
        int index = f.popInt();
        ArrayRef array = (ArrayRef) f.pop();
        f.push(array.data[index]);
    }

    private static void arrayStore(StackFrame f) {
        Object value = f.pop();
        int index = f.popInt();
        ArrayRef array = (ArrayRef) f.pop();
        array.data[index] = value;
    }

    private static ArrayRef newArray(String component, int count) {
        ArrayRef a = new ArrayRef(component, count);
        Object def = componentDefault(component);
        if (def != null) {
            Arrays.fill(a.data, def);
        }
        return a;
    }

    private static Object componentDefault(String component) {
        switch (component) {
            case "boolean": case "byte": case "char": case "short": case "int":
                return 0;
            case "long": return 0L;
            case "float": return 0f;
            case "double": return 0d;
            default: return null;
        }
    }

    private static ArrayRef newMultiArray(String component, int[] counts, int level) {
        int count = counts[level];
        ArrayRef a = new ArrayRef(component, count);
        if (level + 1 < counts.length) {
            for (int i = 0; i < count; i++) {
                a.data[i] = newMultiArray(component, counts, level + 1);
            }
        } else {
            Object def = componentDefault(component);
            if (def != null) Arrays.fill(a.data, def);
        }
        return a;
    }

    private static String componentForAtype(int atype) {
        switch (atype) {
            case 4: return "boolean";
            case 5: return "char";
            case 6: return "float";
            case 7: return "double";
            case 8: return "byte";
            case 9: return "short";
            case 10: return "int";
            case 11: return "long";
            default: throw new VMException("invalid newarray atype " + atype);
        }
    }

    private static boolean isCharArray(ArrayRef a) {
        return "char".equals(a.component);
    }

    private static String charsToString(ArrayRef a) {
        char[] chars = new char[a.data.length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) ((Number) a.data[i]).intValue();
        }
        return new String(chars);
    }

    private static boolean instanceOf(Object v, String className) {
        if (v == null) return false;
        if (v instanceof Obj) {
            return ((Obj) v).className.equals(className);
        }
        if (v instanceof ArrayRef) {
            return className.startsWith("[");
        }
        if (v instanceof String) {
            return "java/lang/String".equals(className) || "java/lang/Object".equals(className);
        }
        if (v instanceof Integer) {
            return "java/lang/Integer".equals(className) || "java/lang/Object".equals(className);
        }
        return "java/lang/Object".equals(className);
    }

    /* ------------------------------------------------------------------ */
    /* Descriptors and values                                             */
    /* ------------------------------------------------------------------ */

    private static List<String> paramTypes(String desc) {
        List<String> out = new ArrayList<>();
        int i = 1;
        while (desc.charAt(i) != ')') {
            int start = i;
            while (desc.charAt(i) == '[') i++;
            if (desc.charAt(i) == 'L') {
                i = desc.indexOf(';', i) + 1;
            } else {
                i++;
            }
            out.add(desc.substring(start, i));
        }
        return out;
    }

    private static String returnType(String desc) {
        return desc.substring(desc.indexOf(')') + 1);
    }

    private static boolean isTwoSlot(Object v) {
        return v instanceof Long || v instanceof Double;
    }

    private static Object popOfType(StackFrame f, String type) {
        return f.pop();
    }

    private static int fcmp(double a, double b, int nanResult) {
        if (Double.isNaN(a) || Double.isNaN(b)) return nanResult;
        return Double.compare(a, b);
    }

    private static void branch(StackFrame f, int opPc, boolean taken) {
        int offset = f.s2();
        if (taken) {
            f.pc = opPc + offset;
        }
    }

    /** Human readable rendering used by print/println/toString. */
    static String valueToString(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return (String) v;
        if (v instanceof ArrayRef) {
            ArrayRef a = (ArrayRef) v;
            if (isCharArray(a)) return charsToString(a);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.data.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(valueToString(a.data[i]));
            }
            return sb.append(']').toString();
        }
        if (v instanceof PrintStreamRef) return "java.io.PrintStream";
        if (v instanceof StringBuilderRef) return ((StringBuilderRef) v).sb.toString();
        if (v instanceof Obj) {
            Obj o = (Obj) v;
            Object custom = o.fields.get("__string");
            return custom != null ? valueToString(custom) : o.className;
        }
        return String.valueOf(v);
    }

    /* ------------------------------------------------------------------ */
    /* Runtime data types                                                 */
    /* ------------------------------------------------------------------ */

    /** A minimal JVM frame: locals, operand stack and program counter. */
    private static final class StackFrame {
        final ClassData owner;
        final MethodData method;
        final ConstantPool cp;
        final byte[] bytecode;
        final Object[] locals;
        final Object[] stack;
        int sp = 0;
        int pc = 0;

        StackFrame(ClassData owner, MethodData method, Object thisRef, Object[] args) {
            this.owner = owner;
            this.method = method;
            this.cp = owner.getConstantPool();
            CodeData cd = method.getCode();
            this.bytecode = cd.getBytecode();

            List<String> types = paramTypes(method.getMethodType());
            int slots = method.getNumLocalVariables();
            int needed = args.length + (thisRef != null ? 1 : 0) + 2;
            if (slots < needed) {
                slots = needed;
            }
            this.locals = new Object[Math.max(slots, 2)];
            this.stack = new Object[Math.max(cd.getNumStackSlots(), 16)];

            int local = 0;
            if (thisRef != null) {
                locals[local++] = thisRef;
            }
            for (int i = 0; i < args.length; i++) {
                locals[local] = args[i];
                local += ("J".equals(types.get(i)) || "D".equals(types.get(i))) ? 2 : 1;
            }
        }

        int u1() {
            return bytecode[pc++] & 0xff;
        }

        int u2() {
            int v = ((bytecode[pc] & 0xff) << 8) | (bytecode[pc + 1] & 0xff);
            pc += 2;
            return v;
        }

        int s2() {
            return (short) u2();
        }

        int s4() {
            int v = ((bytecode[pc] & 0xff) << 24)
                    | ((bytecode[pc + 1] & 0xff) << 16)
                    | ((bytecode[pc + 2] & 0xff) << 8)
                    | (bytecode[pc + 3] & 0xff);
            pc += 4;
            return v;
        }

        void skip2() {
            pc += 2;
        }

        void skip4() {
            pc += 4;
        }

        Object local(int index) {
            return index < locals.length ? locals[index] : null;
        }

        void local(int index, Object value) {
            locals[index] = value;
        }

        int intLocal(int index) {
            Object v = local(index);
            return v == null ? 0 : ((Number) v).intValue();
        }

        void push(Object value) {
            if (sp >= stack.length) {
                throw new VMException("operand stack overflow (" + sp + ") in "
                        + method.getMethodName() + method.getMethodType());
            }
            stack[sp++] = value;
        }

        Object pop() {
            if (sp <= 0) {
                throw new VMException("operand stack underflow in "
                        + method.getMethodName() + method.getMethodType());
            }
            Object v = stack[--sp];
            stack[sp] = null;
            return v;
        }

        Object peek() {
            if (sp <= 0) {
                throw new VMException("operand stack underflow in "
                        + method.getMethodName() + method.getMethodType());
            }
            return stack[sp - 1];
        }

        int popInt() {
            Object v = pop();
            return v == null ? 0 : ((Number) v).intValue();
        }

        long popLong() {
            Object v = pop();
            return v == null ? 0L : ((Number) v).longValue();
        }

        float popFloat() {
            Object v = pop();
            return v == null ? 0f : ((Number) v).floatValue();
        }

        double popDouble() {
            Object v = pop();
            return v == null ? 0d : ((Number) v).doubleValue();
        }
    }

    /** An instance of an interpreted class. */
    private static final class Obj {
        final String className;
        final Map<String, Object> fields = new HashMap<>();

        Obj(String className) {
            this.className = className;
        }
    }

    /** An array value. Numeric components are stored boxed. */
    private static final class ArrayRef {
        final String component;
        Object[] data;

        ArrayRef(String component, int length) {
            this.component = component;
            this.data = new Object[length];
        }
    }

    private static final class PrintStreamRef {
        final PrintStream out;

        PrintStreamRef(PrintStream out) {
            this.out = out;
        }
    }

    private static final class StringBuilderRef {
        StringBuilder sb = new StringBuilder();
    }

    /** Raised for anything the interpreter cannot execute. */
    static final class VMException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        VMException(String message) {
            super(message);
        }
    }
}

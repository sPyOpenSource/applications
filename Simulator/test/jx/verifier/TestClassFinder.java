package jx.verifier;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jx.classfile.ClassData;
import jx.classstore.ClassFinder;

/**
 * A {@link ClassFinder} for tests that resolves classes from a map of
 * programmatically-built class files and/or from a directory of compiled
 * fixtures. The JVM's own classes (other than {@code java/lang/Object}, which
 * the verifier treats as a sentinel and never resolves) are not available, so
 * fixtures must stay within primitives, arrays, and fixture-defined classes.
 */
public class TestClassFinder implements ClassFinder {

    private final Map<String, ClassData> cache = new HashMap<>();
    private final List<Path> roots = new ArrayList<>();

    public TestClassFinder addRoot(Path dir) {
        roots.add(dir);
        return this;
    }

    public TestClassFinder register(String internalName, byte[] classFile) throws IOException {
        cache.put(internalName, parse(new ByteArrayInputStream(classFile)));
        return this;
    }

    @Override
    public ClassData findClass(String className) {
        ClassData cached = cache.get(className);
        if (cached != null) return cached;
        for (Path root : roots) {
            Path f = root.resolve(className + ".class");
            if (Files.isRegularFile(f)) {
                try (InputStream in = Files.newInputStream(f)) {
                    ClassData parsed = parse(in);
                    cache.put(className, parsed);
                    return parsed;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    @Override
    public void dump() {
    }

    private ClassData parse(InputStream in) throws IOException {
        return new ClassData(new DataInputStream(in));
    }
}

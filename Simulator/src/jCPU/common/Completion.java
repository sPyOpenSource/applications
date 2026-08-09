package jCPU.common;

/** Signal produced by evaluating one statement; drives frame unwinding. */
public final class Completion {
    public static final int NORMAL = 0, RETURN = 1, THROW = 2;
    public final int type;
    public final Object value;
    private Completion(int type, Object value) { this.type = type; this.value = value; }
    public static Completion normal() { return new Completion(NORMAL, null); }
    public static Completion returned(Object v) { return new Completion(RETURN, v); }
    public static Completion thrown(Object v) { return new Completion(THROW, v); }
}
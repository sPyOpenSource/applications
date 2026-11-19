package common;

/**
 * Runtime or parse error produced by a tree-walking interpreter, carrying the
 * step counter and 1-based source line at which it occurred.
 */
public class InterpreterException extends RuntimeException {
    private final int step;   // -1 if unknown
    private final int line;   // 1-based source line, -1 if unknown

    public InterpreterException(String msg) { this(msg, -1, -1); }
    public InterpreterException(String msg, int step, int line) {
        super(msg); this.step = step; this.line = line;
    }
    public int getStep() { return step; }
    public int getSourceLine() { return line; }
}

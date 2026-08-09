package jCPU.common;

public class InterpreterException extends RuntimeException {
    private final int step;
    private final int line;

    public InterpreterException(String msg) { this(msg, -1, -1); }
    public InterpreterException(String msg, int step, int line) {
        super(msg); this.step = step; this.line = line;
    }
    public int getStep() { return step; }
    public int getSourceLine() { return line; }
}
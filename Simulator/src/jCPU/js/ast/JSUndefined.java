package jCPU.js.ast;

public final class JSUndefined extends JSExpr {
    public static final JSUndefined INSTANCE = new JSUndefined();
    public JSUndefined() { super(0); }
    public JSUndefined(int line) { super(line); }
}
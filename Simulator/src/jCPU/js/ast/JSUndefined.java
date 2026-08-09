package jCPU.js.ast;

public final class JSUndefined extends JSExpr {
    public static final JSUndefined INSTANCE = new JSUndefined();
    private JSUndefined() { super(0); }
}
package jCPU.js.ast;

public final class JSNull extends JSExpr {
    public static final JSNull INSTANCE = new JSNull();
    private JSNull() { super(0); }
}
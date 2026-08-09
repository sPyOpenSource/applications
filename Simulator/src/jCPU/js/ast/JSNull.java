package jCPU.js.ast;

public final class JSNull extends JSExpr {
    public static final JSNull INSTANCE = new JSNull();
    public JSNull() { super(0); }
    public JSNull(int line) { super(line); }
}
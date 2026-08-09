package jCPU.js.ast;

public final class JSBool extends JSExpr {
    public final boolean v;
    public JSBool(int line, boolean v) {
        super(line);
        this.v = v;
    }
}
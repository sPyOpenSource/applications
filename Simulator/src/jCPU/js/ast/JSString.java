package jCPU.js.ast;

public final class JSString extends JSExpr {
    public final String v;
    public JSString(int line, String v) {
        super(line);
        this.v = v;
    }
}
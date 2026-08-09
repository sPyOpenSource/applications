package jCPU.js.ast;

public final class JSNumber extends JSExpr {
    public final double v;
    public JSNumber(int line, double v) {
        super(line);
        this.v = v;
    }
}
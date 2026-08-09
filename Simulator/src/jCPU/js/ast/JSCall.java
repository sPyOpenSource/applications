package jCPU.js.ast;

public final class JSCall extends JSExpr {
    public final JSExpr callee;
    public final JSExpr[] args;
    public JSCall(int line, JSExpr callee, JSExpr[] args) {
        super(line);
        this.callee = callee;
        this.args = args;
    }
}
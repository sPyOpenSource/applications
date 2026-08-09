package jCPU.js.ast;

public final class JSConditional extends JSExpr {
    public final JSExpr cond;
    public final JSExpr thenE;
    public final JSExpr elseE;
    public JSConditional(int line, JSExpr cond, JSExpr thenE, JSExpr elseE) {
        super(line);
        this.cond = cond;
        this.thenE = thenE;
        this.elseE = elseE;
    }
}
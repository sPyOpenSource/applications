package jCPU.js.ast;

public final class JSLogical extends JSExpr {
    public final String op;
    public final JSExpr left;
    public final JSExpr right;
    public JSLogical(int line, String op, JSExpr left, JSExpr right) {
        super(line);
        this.op = op;
        this.left = left;
        this.right = right;
    }
}
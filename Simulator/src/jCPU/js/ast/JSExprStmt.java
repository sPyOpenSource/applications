package jCPU.js.ast;

public final class JSExprStmt extends JSStmt {
    public final JSExpr expr;
    public JSExprStmt(int line, JSExpr expr) {
        super(line);
        this.expr = expr;
    }
}
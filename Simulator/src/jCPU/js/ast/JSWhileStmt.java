package jCPU.js.ast;

public final class JSWhileStmt extends JSStmt {
    public final JSExpr cond;
    public final JSStmt body;
    public JSWhileStmt(int line, JSExpr cond, JSStmt body) {
        super(line);
        this.cond = cond;
        this.body = body;
    }
}
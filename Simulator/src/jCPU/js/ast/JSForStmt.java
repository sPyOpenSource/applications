package jCPU.js.ast;

public final class JSForStmt extends JSStmt {
    public final JSStmt init;
    public final JSExpr cond;
    public final JSExpr step;
    public final JSStmt body;
    public JSForStmt(int line, JSStmt init, JSExpr cond, JSExpr step, JSStmt body) {
        super(line);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }
}
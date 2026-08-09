package jCPU.js.ast;

public final class JSIfStmt extends JSStmt {
    public final JSExpr cond;
    public final JSStmt thenStmt;
    public final JSStmt elseStmt;
    public JSIfStmt(int line, JSExpr cond, JSStmt thenStmt, JSStmt elseStmt) {
        super(line);
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }
}
package jCPU.js.ast;

public final class JSBlock extends JSStmt {
    public final JSStmt[] stmts;
    public JSBlock(int line, JSStmt[] stmts) {
        super(line);
        this.stmts = stmts;
    }
}
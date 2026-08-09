package jCPU.js.ast;

public final class JSProgram extends JSNode {
    public final JSStmt[] stmts;
    public JSProgram(int line, JSStmt[] stmts) {
        super(line);
        this.stmts = stmts;
    }
}
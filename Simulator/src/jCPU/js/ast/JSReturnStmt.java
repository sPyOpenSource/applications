package jCPU.js.ast;

public final class JSReturnStmt extends JSStmt {
    public final JSExpr value;
    public JSReturnStmt(int line, JSExpr value) {
        super(line);
        this.value = value;
    }
}
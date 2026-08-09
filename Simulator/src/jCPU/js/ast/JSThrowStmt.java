package jCPU.js.ast;

public final class JSThrowStmt extends JSStmt {
    public final JSExpr value;
    public JSThrowStmt(int line, JSExpr value) {
        super(line);
        this.value = value;
    }
}
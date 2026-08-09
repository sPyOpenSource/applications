package jCPU.js.ast;

public final class JSVarDecl extends JSStmt {
    public final String name;
    public final JSExpr init;
    public JSVarDecl(int line, String name, JSExpr init) {
        super(line);
        this.name = name;
        this.init = init;
    }
}
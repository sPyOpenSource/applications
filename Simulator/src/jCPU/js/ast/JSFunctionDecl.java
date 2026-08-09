package jCPU.js.ast;

public final class JSFunctionDecl extends JSStmt {
    public final String name;
    public final String[] params;
    public final JSBlock body;
    public JSFunctionDecl(int line, String name, String[] params, JSBlock body) {
        super(line);
        this.name = name;
        this.params = params;
        this.body = body;
    }
}
package jCPU.js.ast;

public final class JSFunctionExpr extends JSExpr {
    public final String[] params;
    public final JSBlock body;
    public JSFunctionExpr(int line, String[] params, JSBlock body) {
        super(line);
        this.params = params;
        this.body = body;
    }
}
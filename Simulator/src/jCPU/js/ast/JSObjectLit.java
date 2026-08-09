package jCPU.js.ast;

public final class JSObjectLit extends JSExpr {
    public final String[] keys;
    public final JSExpr[] values;
    public JSObjectLit(int line, String[] keys, JSExpr[] values) {
        super(line);
        this.keys = keys;
        this.values = values;
    }
}
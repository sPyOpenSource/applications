package jCPU.js.ast;

public final class JSIndex extends JSExpr {
    public final JSExpr obj;
    public final JSExpr index;
    public JSIndex(int line, JSExpr obj, JSExpr index) {
        super(line);
        this.obj = obj;
        this.index = index;
    }
}
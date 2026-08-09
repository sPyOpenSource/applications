package jCPU.js.ast;

public final class JSAssign extends JSExpr {
    public final JSExpr target;
    public final JSExpr value;
    public JSAssign(int line, JSExpr target, JSExpr value) {
        super(line);
        this.target = target;
        this.value = value;
    }
}
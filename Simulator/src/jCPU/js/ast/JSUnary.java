package jCPU.js.ast;

public final class JSUnary extends JSExpr {
    public final String op;
    public final JSExpr operand;
    public JSUnary(int line, String op, JSExpr operand) {
        super(line);
        this.op = op;
        this.operand = operand;
    }
}
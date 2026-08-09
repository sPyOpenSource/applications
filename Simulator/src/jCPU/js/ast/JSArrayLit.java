package jCPU.js.ast;

public final class JSArrayLit extends JSExpr {
    public final JSExpr[] elements;
    public JSArrayLit(int line, JSExpr[] elements) {
        super(line);
        this.elements = elements;
    }
}
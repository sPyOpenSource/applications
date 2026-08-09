package jCPU.js.ast;

public final class JSIdent extends JSExpr {
    public final String name;
    public JSIdent(int line, String name) {
        super(line);
        this.name = name;
    }
}
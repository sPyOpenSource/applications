package jCPU.js.ast;

public final class JSMember extends JSExpr {
    public final JSExpr obj;
    public final String prop;
    public JSMember(int line, JSExpr obj, String prop) {
        super(line);
        this.obj = obj;
        this.prop = prop;
    }
}
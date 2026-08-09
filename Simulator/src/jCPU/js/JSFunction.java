package jCPU.js;

/** Lightweight function value. The interpreter fills details (closure) when used. */
public final class JSFunction {
    public final String name;
    public final int arity;      // -1 for variadic builtins
    public final boolean builtin;
    public Object decl;          // jCPU.js.ast.FunctionDecl when user-defined (set by interpreter)
    public JSEnvironment closure;
    public JSFunction(String name, int arity, boolean builtin) {
        this.name = name; this.arity = arity; this.builtin = builtin;
    }
    @Override public String toString() { return "<function " + name + ">"; }
}
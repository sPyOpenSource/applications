package jCPU.js;

import java.util.HashMap;
import java.util.Map;

/** Lexical environment built from the enclosing scopes. */
public final class JSEnvironment {
    public final JSEnvironment parent;
    private final Map<String, JSValue> vars = new HashMap<>();

    public JSEnvironment(JSEnvironment parent) { this.parent = parent; }

    public void declare(String name, JSValue v) { vars.put(name, v); }

    /** Assign into the nearest enclosing scope that has the name; else declare here. */
    public void define(String name, JSValue v) {
        JSEnvironment e = this;
        while (e != null && !e.vars.containsKey(name)) e = e.parent;
        (e != null ? e : this).vars.put(name, v);
    }

    public boolean isDefinedInScope(String name) { return vars.containsKey(name); }

    public JSValue lookup(String name) {
        for (JSEnvironment e = this; e != null; e = e.parent)
            if (e.vars.containsKey(name)) return e.vars.get(name);
        return JSValue.UNDEFINED;
    }

    public Map<String, JSValue> snapshot() { return new HashMap<>(vars); }
}
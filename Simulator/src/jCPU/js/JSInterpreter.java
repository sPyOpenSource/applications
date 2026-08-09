package jCPU.js;

import jCPU.common.*;
import jCPU.js.ast.*;
import java.util.ArrayList;
import java.util.List;

public class JSInterpreter extends InterpreterBase implements jCPU.iCPU {
    public static final int MAX_DEPTH = 10000;

    private int depth = 0;
    private final List<String> output = new ArrayList<>();

    public String readOut() {
        StringBuilder sb = new StringBuilder();
        for (String l : output) sb.append(l).append('\n');
        return sb.toString();
    }
    protected void writeOut(String s) { System.out.println(s); output.add(s); }

    @Override
    public void loadSource(String src) {
        super.loadSource(src);
        JSProgram p;
        try { p = JSParser.parse(src); }
        catch (RuntimeException e) { throw new InterpreterException("Parse error: " + e.getMessage(), 0, -1); }
        JSEnvironment env = new JSEnvironment(null);
        installBuiltins(env);
        pushFrame((Object[]) p.stmts, env);
        state = STATE_RUNNING;
    }

    private void installBuiltins(JSEnvironment env) {
        env.declare("print", wrapFunction(new JSFunction("print", -1, true)));
        env.declare("len", wrapFunction(new JSFunction("len", 1, true)));
        env.declare("typeof", wrapFunction(new JSFunction("typeof", 1, true)));
    }

    private JSValue wrapFunction(JSFunction fn) {
        return new JSValue() {
            @Override public int type() { return JSValue.T_OBJ; }
            public final JSFunction func = fn;
            @Override public String toString() { return fn.toString(); }
        };
    }

    private JSFunction unwrapFunction(JSValue v) {
        try {
            java.lang.reflect.Field f = v.getClass().getDeclaredField("func");
            f.setAccessible(true);
            return (JSFunction) f.get(v);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public int step() throws InterpreterException {
        if (frame == null) { state = STATE_ENDED; return -1; }
        JSStmt[] code = (JSStmt[]) frame.code;
        if (frame.pc >= code.length) {
            Frame popped = frame; frame = frame.parent;
            if (frame == null) { state = STATE_ENDED; return -1; }
            return step();
        }
        JSStmt stmt = code[frame.pc];
        Completion c = execStmt(stmt, (JSEnvironment) frame.env);
        if (c.type == Completion.THROW)
            throw new InterpreterException(String.valueOf(c.value), (int) steps, stmt.line);
        if (c.type == Completion.RETURN) {
            frame = frame.parent;
            if (frame == null) { state = STATE_ENDED; return -1; }
            frame.pc++;
            frame.pending = c.value;
            return frame.pc;
        }
        frame.pc++;
        return frame.pc;
    }

    // ---- core eval ----

    private Completion execStmt(JSStmt s, JSEnvironment env) {
        if (s instanceof JSVarDecl) return execVar((JSVarDecl) s, env);
        if (s instanceof JSExprStmt) return execExprStmt((JSExprStmt) s, env);
        if (s instanceof JSBlock) return execBlock((JSBlock) s, env);
        if (s instanceof JSIfStmt) return execIf((JSIfStmt) s, env);
        if (s instanceof JSWhileStmt) return execWhile((JSWhileStmt) s, env);
        if (s instanceof JSForStmt) return execFor((JSForStmt) s, env);
        if (s instanceof JSFunctionDecl) return execFuncDecl((JSFunctionDecl) s, env);
        if (s instanceof JSReturnStmt) return Completion.returned(evalExpr(((JSReturnStmt) s).value, env));
        if (s instanceof JSBreakStmt) throw new InterpreterException("break outside loop", (int) steps, s.line);
        if (s instanceof JSContinueStmt) throw new InterpreterException("continue outside loop", (int) steps, s.line);
        if (s instanceof JSTryStmt) return execTry((JSTryStmt) s, env);
        throw new InterpreterException("Unsupported statement", (int) steps, s.line);
    }

    private Completion execBlock(JSBlock b, JSEnvironment env) {
        for (JSStmt st : b.stmts) { Completion c = execStmt(st, env); if (c.type != Completion.NORMAL) return c; }
        return Completion.normal();
    }

    private Completion execVar(JSVarDecl d, JSEnvironment env) {
        JSValue v = d.init != null ? evalExpr(d.init, env) : JSValue.UNDEFINED;
        env.declare(d.name, v);
        return Completion.normal();
    }

    private Completion execExprStmt(JSExprStmt s, JSEnvironment env) {
        evalExpr(s.expr, env);
        return Completion.normal();
    }

    private Completion execIf(JSIfStmt s, JSEnvironment env) {
        JSValue c = evalExpr(s.cond, env);
        boolean truthy = isTruthy(c);
        return execStmt(truthy ? s.thenStmt : s.elseStmt, env);
    }

    private Completion execWhile(JSWhileStmt s, JSEnvironment env) {
        while (isTruthy(evalExpr(s.cond, env))) {
            Completion c = execStmt(s.body, env);
            if (c.type == Completion.RETURN || c.type == Completion.THROW) return c;
        }
        return Completion.normal();
    }

    private Completion execFor(JSForStmt s, JSEnvironment env) {
        if (s.init != null) execStmt(s.init, env);
        while (s.cond == null || isTruthy(evalExpr(s.cond, env))) {
            Completion c = execStmt(s.body, env);
            if (c.type == Completion.RETURN || c.type == Completion.THROW) return c;
            if (s.step != null) evalExpr(s.step, env);
        }
        return Completion.normal();
    }

    private Completion execFuncDecl(JSFunctionDecl d, JSEnvironment env) {
        JSFunction fn = new JSFunction(d.name, d.params.length, false);
        fn.decl = d;
        fn.closure = env;
        env.declare(d.name, wrapFunction(fn));
        return Completion.normal();
    }

    private Completion execTry(JSTryStmt s, JSEnvironment env) {
        try {
            return execStmt(s.tryBlock, env);
        } catch (InterpreterException e) {
            if (s.catchBlock != null && s.catchVar != null) {
                JSEnvironment catchEnv = new JSEnvironment(env);
                catchEnv.declare(s.catchVar, new JSValue.Str(e.getMessage()));
                return execStmt(s.catchBlock, catchEnv);
            }
            throw e;
        }
    }

    // ---- expression evaluation ----

    private JSValue evalExpr(JSExpr e, JSEnvironment env) {
        if (e == null) return JSValue.UNDEFINED;
        if (e instanceof JSNumber) return new JSValue.Num(((JSNumber) e).v);
        if (e instanceof JSString) return new JSValue.Str(((JSString) e).v);
        if (e instanceof JSBool) return new JSValue.Bool(((JSBool) e).v);
        if (e instanceof JSNull) return JSValue.NULL;
        if (e instanceof JSUndefined) return JSValue.UNDEFINED;
        if (e instanceof JSIdent) return env.lookup(((JSIdent) e).name);
        if (e instanceof JSBinary) return evalBinary((JSBinary) e, env);
        if (e instanceof JSLogical) return evalLogical((JSLogical) e, env);
        if (e instanceof JSUnary) return evalUnary((JSUnary) e, env);
        if (e instanceof JSAssign) return evalAssign((JSAssign) e, env);
        if (e instanceof JSCall) return evalCall((JSCall) e, env);
        if (e instanceof JSMember) return evalMember((JSMember) e, env);
        if (e instanceof JSIndex) return evalIndex((JSIndex) e, env);
        if (e instanceof JSObjectLit) return evalObjectLit((JSObjectLit) e, env);
        if (e instanceof JSArrayLit) return evalArrayLit((JSArrayLit) e, env);
        if (e instanceof JSFunctionExpr) return evalFunctionExpr((JSFunctionExpr) e, env);
        if (e instanceof JSConditional) {
            JSValue c = evalExpr(((JSConditional) e).cond, env);
            return isTruthy(c) ? evalExpr(((JSConditional) e).thenE, env) : evalExpr(((JSConditional) e).elseE, env);
        }
        return JSValue.UNDEFINED;
    }

    private JSValue evalFunctionExpr(JSFunctionExpr e, JSEnvironment env) {
        JSFunction fn = new JSFunction("<anonymous>", e.params.length, false);
        fn.decl = new JSFunctionDecl(e.line, "<anonymous>", e.params, e.body);
        fn.closure = env;
        return wrapFunction(fn);
    }

    private JSValue evalBinary(JSBinary e, JSEnvironment env) {
        JSValue l = evalExpr(e.left, env);
        JSValue r = evalExpr(e.right, env);
        String op = e.op;
        double lv = l instanceof JSValue.Num ? ((JSValue.Num) l).v : 0;
        double rv = r instanceof JSValue.Num ? ((JSValue.Num) r).v : 0;
        if (op.equals("+")) {
            if (l instanceof JSValue.Str || r instanceof JSValue.Str)
                return new JSValue.Str(l.toString() + r.toString());
            return new JSValue.Num(lv + rv);
        }
        if (op.equals("-")) return new JSValue.Num(lv - rv);
        if (op.equals("*")) return new JSValue.Num(lv * rv);
        if (op.equals("/")) return new JSValue.Num(lv / rv);
        if (op.equals("%")) return new JSValue.Num(lv % rv);
        if (op.equals("==")) return new JSValue.Bool(l.toString().equals(r.toString()));
        if (op.equals("!=")) return new JSValue.Bool(!l.toString().equals(r.toString()));
        if (op.equals("<")) return new JSValue.Bool(lv < rv);
        if (op.equals(">")) return new JSValue.Bool(lv > rv);
        if (op.equals("<=")) return new JSValue.Bool(lv <= rv);
        if (op.equals(">=")) return new JSValue.Bool(lv >= rv);
        return JSValue.UNDEFINED;
    }

    private JSValue evalLogical(JSLogical e, JSEnvironment env) {
        JSValue l = evalExpr(e.left, env);
        if ("&&".equals(e.op)) return isTruthy(l) ? evalExpr(e.right, env) : new JSValue.Bool(false);
        if ("||".equals(e.op)) return isTruthy(l) ? new JSValue.Bool(true) : evalExpr(e.right, env);
        return JSValue.UNDEFINED;
    }

    private JSValue evalUnary(JSUnary e, JSEnvironment env) {
        JSValue v = evalExpr(e.operand, env);
        if (e.op.equals("-")) return new JSValue.Num(-(v instanceof JSValue.Num ? ((JSValue.Num) v).v : 0));
        if (e.op.equals("!")) return new JSValue.Bool(!isTruthy(v));
        if (e.op.equals("+")) return v;
        if (e.op.equals("++") && e.operand instanceof JSIdent) {
            String name = ((JSIdent) e.operand).name;
            JSValue cur = env.lookup(name);
            double nv = cur instanceof JSValue.Num ? ((JSValue.Num) cur).v : 0;
            env.define(name, new JSValue.Num(nv + 1));
            return v;
        }
        if (e.op.equals("--") && e.operand instanceof JSIdent) {
            String name = ((JSIdent) e.operand).name;
            JSValue cur = env.lookup(name);
            double nv = cur instanceof JSValue.Num ? ((JSValue.Num) cur).v : 0;
            env.define(name, new JSValue.Num(nv - 1));
            return v;
        }
        return v;
    }

    private JSValue evalAssign(JSAssign e, JSEnvironment env) {
        JSValue v = evalExpr(e.value, env);
        if (e.target instanceof JSIdent) {
            env.define(((JSIdent) e.target).name, v);
            return v;
        }
        if (e.target instanceof JSMember) {
            JSValue o = evalExpr(((JSMember) e.target).obj, env);
            if (o instanceof JSValue.Obj) ((JSValue.Obj) o).set(((JSMember) e.target).prop, v);
            return v;
        }
        if (e.target instanceof JSIndex) {
            JSValue o = evalExpr(((JSIndex) e.target).obj, env);
            JSValue i = evalExpr(((JSIndex) e.target).index, env);
            if (o instanceof JSValue.Obj) ((JSValue.Obj) o).set(i.toString(), v);
            return v;
        }
        return v;
    }

    private JSValue evalCall(JSCall e, JSEnvironment env) {
        JSValue callee = evalExpr(e.callee, env);
        List<JSValue> args = new ArrayList<>();
        for (JSExpr a : e.args) args.add(evalExpr(a, env));
        if (callee instanceof JSValue) {
            JSFunction fn = unwrapFunction(callee);
            if (fn != null) {
                if (fn.builtin) return callBuiltin(fn.name, args);
                JSFunctionDecl decl = (JSFunctionDecl) fn.decl;
                JSEnvironment newEnv = new JSEnvironment(fn.closure);
                for (int i = 0; i < decl.params.length; i++)
                    newEnv.declare(decl.params[i], i < args.size() ? args.get(i) : JSValue.UNDEFINED);
                Completion c = execBlock(decl.body, newEnv);
                if (c.type == Completion.RETURN) return (JSValue) c.value;
                return JSValue.UNDEFINED;
            }
        }
        return JSValue.UNDEFINED;
    }

    private JSValue evalMember(JSMember e, JSEnvironment env) {
        JSValue o = evalExpr(e.obj, env);
        if (o instanceof JSValue.Obj) return ((JSValue.Obj) o).get(e.prop);
        return JSValue.UNDEFINED;
    }

    private JSValue evalIndex(JSIndex e, JSEnvironment env) {
        JSValue o = evalExpr(e.obj, env);
        JSValue k = evalExpr(e.index, env);
        if (o instanceof JSValue.Obj) return ((JSValue.Obj) o).get(k.toString());
        return JSValue.UNDEFINED;
    }

    private JSValue evalObjectLit(JSObjectLit e, JSEnvironment env) {
        JSValue.Obj o = new JSValue.Obj();
        for (int i = 0; i < e.keys.length; i++) o.set(e.keys[i], evalExpr(e.values[i], env));
        return o;
    }

    private JSValue evalArrayLit(JSArrayLit e, JSEnvironment env) {
        JSValue.Obj o = new JSValue.Obj();
        for (int i = 0; i < e.elements.length; i++) o.set(String.valueOf(i), evalExpr(e.elements[i], env));
        o.set("length", new JSValue.Num(e.elements.length));
        return o;
    }

    private JSValue callBuiltin(String name, List<JSValue> args) {
        if ("print".equals(name)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.size(); i++) { if (i > 0) sb.append(' '); sb.append(args.get(i).toString()); }
            writeOut(sb.toString());
            return JSValue.UNDEFINED;
        }
        if ("len".equals(name)) {
            if (!args.isEmpty()) {
                JSValue v = args.get(0);
                if (v instanceof JSValue.Str) return new JSValue.Num(v.toString().length());
                if (v instanceof JSValue.Obj) return new JSValue.Num(((JSValue.Obj) v).props.size());
            }
            return new JSValue.Num(0);
        }
        if ("typeof".equals(name)) {
            if (!args.isEmpty()) return new JSValue.Str(args.get(0).getClass().getSimpleName().toLowerCase());
            return new JSValue.Str("undefined");
        }
        return JSValue.UNDEFINED;
    }

    private boolean isTruthy(JSValue v) {
        if (v == JSValue.NULL || v == JSValue.UNDEFINED) return false;
        if (v instanceof JSValue.Bool) return ((JSValue.Bool) v).b;
        if (v instanceof JSValue.Num) return ((JSValue.Num) v).v != 0;
        if (v instanceof JSValue.Str) return !v.toString().isEmpty();
        return true;
    }
}
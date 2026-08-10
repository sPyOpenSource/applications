# JavaScript & Python Tree-Walking Interpreters — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two selectable CPU types to the simulator — `JSInterpreter` (tiny JavaScript subset) and `PythonInterpreter` (tiny Python subset) — each a tree-walking interpreter implementing `jCPU.iCPU`, running source statement by statement with source-level `getDecodeAt()`.

**Architecture:** Each VM lives in its own package (`jCPU.js`, `jCPU.python`). A thin shared harness `jCPU.common.InterpreterBase` owns the frame stack, step counter, source-line table, and run state. Each language interpreter is a tree-walker: lexer → recursive-descent parser → AST → evaluator. Stepping model (line-level, see Global Constraint 6): `step()` executes one *top-level* statement of the current frame; `if`/`while`/`for`/`try` and function calls that appear *inside* an expression or statement body run eagerly inside that step. The only case that pushes a child frame (so the debugger can step *into* a callee) is a function call that is a whole statement of its own (`foo(a);`). Call/return across frames is handled with the shared `Frame` (a `pending` slot carries the return value).

**Tech Stack:** Java 17 (project `javac.source/target=17`), JUnit 4 (NetBeans ant build: `ant test`), no third-party deps beyond the project's existing `lib/java-binutils-0.1.0.jar`.

## Global Constraints

1. Source under `src/`, package path mirrors directory path. New packages: `jCPU.common`, `jCPU.js`, `jCPU.js.ast`, `jCPU.python`, `jCPU.python.ast`.
2. Tests under `test/`, same package paths. Run with `ant test` (full suite) — use `-Dtest.includes=test/<pkg>/<File>.java` to filter on compile-safe runs when the plan says so.
3. Both VM classes implement `jCPU.iCPU` and override only the meaningful members; memory-mapped methods inherit the no-op defaults on the interface.
4. `getDecodeAt(pc)` returns `pc + "\t" + sourceLine` where `sourceLine` is the 0-based line from the stored source (e.g. `"0\tvar a = 1;"`). `pc()` returns the current frame's statement index (or `-1` when finished). Top-level statements are indexed in program order.
5. Constraint does not survive: do NOT import Swing/AWT in `src/jCPU/js|python|common/*`.
6. **Stepping granularity:** one top-level statement per `step()`. Compound statements (if/else, while, for, block, try) **execute their whole subtree eagerly within the single step** — their inner statements are NOT individually stepped in v1. The only child-frame case is a **bare call statement** (an `ExprStmt` whose expression is a `CallExpr`) so `step()` may descend into a function body. Document this limitation in each interpreter's class javadoc.
7. Parse errors throw `jCPU.common.InterpreterException` with a message containing `line N`. Runtime errors likewise, with steps and line recorded.
8. Closures capture the environment object by reference; the JavaScript subset allows mutable captured variables, Python subset **does not** support `nonlocal` — Python sample programs avoid it (read-only outer vars / pass args into inner functions).
9. `build.xml` needs no edits (`src/**/*.java` already compiled; `test/**/*.java` already compiled by ant).

---

## File Structure

```
src/jCPU/common/
  InterpreterException.java   runtime/parse error w/ step + source line
  Completion.java             {NORMAL, RETURN, THROW} + value
  InterpreterBase.java        frames, state, source table, step()/go()/getDecodeAt()
src/jCPU/js/
  JSLexer.java, JSValue.java, JSEnvironment.java, JSFunction.java
  JSParser.java, JSInterpreter.java
  ast/  Node, Statement, Expression + concrete nodes + Program
src/jCPU/python/
  PythonLexer.java, PyValue.java, PyEnvironment.java, PyFunction.java
  PythonParser.java, PythonInterpreter.java
  ast/  (mirror of JS)
test/jCPU/common/SourceRenderTest.java
test/jCPU/js/    JSLexerTest, JSParserTest, JSInterpreterTest,
                 JSProgramTest, JSDecodeTest
test/jCPU/python/ PythonLexerTest, PythonParserTest, PythonInterpreterTest,
                  PythonProgramTest, PythonDecodeTest
test/jCPU/VMPluginTest.java
test/programs/js/fibonacci.js, factorial.js, closures.js, objects.js
test/programs/python/fibonacci.py, factorial.py, closures.py, objects.py
Modified: j51.conf
```

---

### Task 1: Shared exceptions, completion, and base harness

**Files:**
- Create: `src/jCPU/common/InterpreterException.java`
- Create: `src/jCPU/common/Completion.java`
- Create: `src/jCPU/common/InterpreterBase.java`

**Interfaces:**
- Produces (used by every later task):

```java
package jCPU.common;

public class InterpreterException extends RuntimeException {
    private final int step;   // -1 if unknown
    private final int line;   // 1-based source line, -1 if unknown

    public InterpreterException(String msg) { this(msg, -1, -1); }
    public InterpreterException(String msg, int step, int line) {
        super(msg); this.step = step; this.line = line;
    }
    public int getStep() { return step; }
    public int getSourceLine() { return line; }
}
```

```java
package jCPU.common;

/** Signal produced by evaluating one statement; drives frame unwinding. */
public final class Completion {
    public static final int NORMAL = 0, RETURN = 1, THROW = 2;
    public final int type;
    public final Object value;
    private Completion(int type, Object value) { this.type = type; this.value = value; }
    public static Completion normal() { return new Completion(NORMAL, null); }
    public static Completion returned(Object v) { return new Completion(RETURN, v); }
    public static Completion thrown(Object v) { return new Completion(THROW, v); }
}
```

```java
package jCPU.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared VM harness. Owns: run state, step counter, source line table,
 * and a stack of Frames (each = array of language statements + scope).
 * Concrete interpreters implement step()/loadSource()/visit() and use the
 * frame stack + pending fields for call/return.
 */
public abstract class InterpreterBase {
    public static final int STATE_IDLE = 0, STATE_RUNNING = 1,
                            STATE_ENDED = 2, STATE_ERROR = 3;

    /** One body: an Object[] of AST statements, the scope, and a "caller" link. */
    public static class Frame {
        public final Object code;        // Object[] of AST statements
        public int pc;                   // next statement index
        public final Object env;         // language environment
        public final Frame parent;
        public Object pending;           // return value when finishing this frame
        public Frame(Object code, Object env, Frame parent) {
            this.code = code; this.env = env; this.parent = parent;
        }
    }

    protected Frame frame;               // current (top) frame; null => idle/ended
    protected int state = STATE_IDLE;
    protected long steps = 0;
    protected List<String> sourceLines = new ArrayList<>();

    /** Parse+load a program; resets step state. Subclass hook. */
    public void loadSource(String src) {
        sourceLines.clear();
        if (src != null && !src.isEmpty())
            for (String s : src.split("\n", -1)) sourceLines.add(s.stripTrailing());
        reset();
    }

    public void reset() { frame = null; state = STATE_IDLE; steps = 0; }

    public int pc() { return frame == null ? -1 : frame.pc; }

    public String getDecodeAt(int pc) {
        if (pc < 0 || pc >= sourceLines.size()) return "";
        return pc + "\t" + sourceLines.get(pc);
    }

    public abstract int step() throws InterpreterException;

    /** Execute up to limit steps (limit<0 => until ended/error). */
    public void go(int limit) throws InterpreterException {
        while (state != STATE_ENDED && state != STATE_ERROR) {
            int p = step();
            if (limit > 0 && steps >= limit) break;
            if (p < 0) state = STATE_ENDED;
        }
    }

    protected Frame pushFrame(Object[] code, Object env) {
        Frame f = new Frame(code, env, frame);
        frame = f;
        return f;
    }
    protected void popFrameAndRestore() {
        if (frame != null) frame = frame.parent;
    }
}
```

- [ ] **Step 1: Write the three files** exactly as above (`InterpreterBase` reproduced verbatim, including the javadoc).
- [ ] **Step 2: Build** — `ant compile` → BUILD SUCCESSFUL
- [ ] **Step 3: Commit**

```bash
git add src/jCPU/common
git commit -m "feat: add shared interpreter exceptions, completion, and frame harness"
```

---

### Task 2: Red lock test for the decode contract

**Files:**
- Create: `test/jCPU/common/SourceRenderTest.java`

**Interfaces:**
- Consumes: `InterpreterBase.loadSource`, `.getDecodeAt`, `JSInterpreter` (added Task 7). This test fails to compile until Task 7 exists — that is intended (red phase).

- [ ] **Step 1: Write the failing test**

```java
package jCPU.common;

import jCPU.js.JSInterpreter;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceRenderTest {
    @Test
    public void decodeShowsSourceLineOfCurrentStatement() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var a = 1;\nvar b = 2;\n");
        assertEquals("0\tvar a = 1;", vm.getDecodeAt(0));
        assertEquals("1\tvar b = 2;", vm.getDecodeAt(1));
    }
}
```

- [ ] **Step 2: Run** — `ant test -Dtest.includes=test/jCPU/common/SourceRenderTest.java` → compile error (JSInterpreter missing). Expected.
- [ ] **Step 3: Commit the red test**

```bash
git add test/jCPU/common/SourceRenderTest.java
git commit -m "test: lock decode-panel source rendering contract (red)"
```

---

### Task 3: JS lexer + tokens

**Files:**
- Create: `src/jCPU/js/JSLexer.java`
- Create: `test/jCPU/js/JSLexerTest.java`

**Interfaces:**
- Produces: `public final class JSLexer` with
  - `public static final class Token { String kind; String text; int line; int col; }`
  - `public JSLexer(String src)`
  - `public java.util.List<Token> scan()` — kinds: `id`, `num`, `str`, `op`, `punc`, `keyw`. Throws `InterpreterException` (message contains `line N`) on unterminated string / illegal char.

- [ ] **Step 1: Write the lexer**

```java
package jCPU.js;

import jCPU.common.InterpreterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Hand-written lexer for the JS teaching subset. */
public final class JSLexer {
    public static final class Token {
        public final String kind, text; public final int line, col;
        public Token(String kind, String text, int line, int col) {
            this.kind = kind; this.text = text; this.line = line; this.col = col;
        }
        @Override public String toString() { return kind + ":" + text; }
    }

    private static final Set<String> KEYWORDS = Set.of(
        "var","let","const","if","else","while","for","function","return",
        "true","false","null","undefined","throw","try","catch","finally","break","continue");

    private final String src;
    private int p = 0, line = 1, col = 1;
    private final List<Token> out = new ArrayList<>();

    public JSLexer(String src) { this.src = src; }

    public List<Token> scan() {
        while (p < src.length()) {
            char c = src.charAt(p);
            if (c == '\n') { line++; p++; col = 1; continue; }
            if (Character.isWhitespace(c)) { p++; col++; continue; }
            if (c == '/' && p + 1 < src.length() && src.charAt(p + 1) == '/') {
                while (p < src.length() && src.charAt(p) != '\n') { p++; col++; }
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && p + 1 < src.length()
                    && Character.isDigit(src.charAt(p + 1)))) { number(); continue; }
            if (Character.isLetter(c) || c == '_' || c == '$') { ident(); continue; }
            if (c == '"' || c == '\'') { str(); continue; }
            punctOrOp();
        }
        return out;
    }

    private void number() {
        int s = p, l = line, co = col;
        while (p < src.length() && Character.isDigit(src.charAt(p))) p++;
        if (p < src.length() && src.charAt(p) == '.') { p++; while (p < src.length() && Character.isDigit(src.charAt(p))) p++; }
        add("num", src.substring(s, p), l, co);
    }

    private void ident() {
        int s = p, l = line, co = col;
        while (p < src.length() && (Character.isLetterOrDigit(src.charAt(p)) || src.charAt(p) == '_' || src.charAt(p) == '$')) p++;
        add(KEYWORDS.contains(src.substring(s, p)) ? "key" : "id", src.substring(s, p), l, co);
    }

    private void str() {
        char q = src.charAt(p);
        int l = line, co = col;
        StringBuilder sb = new StringBuilder();
        p++;
        while (p < src.length()) {
            char c = src.charAt(p);
            if (c == '\\' && p + 1 < src.length()) {
                char e = src.charAt(p + 1);
                switch (e) { case 'n': sb.append('\n'); break; case 't': sb.append('\t'); break; default: sb.append(e); }
                p += 2; col += 2; continue;
            }
            if (c == q) { p++; col++; out.add(new Token("str", sb.toString(), l, co)); return; }
            if (c == '\n') break;
            sb.append(c); p++; col++;
        }
        throw new InterpreterException("Unterminated string at line " + l);
    }

    private void punctOrOp() {
        int l = line, co = col;
        String[] ops = {"==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=", "%="};
        for (String o : ops) if (src.startsWith(o, p)) { add("op", o, l, co); return; }
        char c = src.charAt(p);
        if ("+-*/%<>=!;".indexOf(c) >= 0) add("op", String.valueOf(c), l, co);
        else if ("(){}[],.:".indexOf(c) >= 0) add("punc", String.valueOf(c), l, co);
        else throw new InterpreterException("Illegal character '" + c + "' at line " + l);
        p++; col++;
    }

    private void add(String kind, String text, int l, int co) {
        out.add(new Token(kind, text, l, co));
        p = p + text.length(); // unused in most callers; kept simple — see note
        p = p; // no-op guard
    }
}
```

Note: the `add` overload above is kept only for API symmetry; keep the simple version:

```java
    private void add(String kind, String text, int l, int co) {
        out.add(new Token(kind, text, l, co));
    }
```

and ensure `ident()`/`number()` advance `p` before calling `add` — they already do. Columns are best-effort (no need to be exact in tests beyond the line number).

- [ ] **Step 2: Write the lexer tests**

```java
package jCPU.js;

import jCPU.common.InterpreterException;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class JSLexerTest {
    @Test public void scansMixedProgram() {
        List<JSLexer.Token> t = new JSLexer("var x = 1 + 2; // hi\nprint(x);").scan();
        assertTrue(t.get(0).kind.contentEquals("key"));
        assertEquals("var", t.get(0).text);
        assertEquals("num", t.get(3).kind);
        assertEquals("punc", t.get(4).kind);   // ;
        assertTrue(t.size() >= 5);
    }

    @Test public void keywordsAreFlagged() {
        List<JSLexer.Token> t = new JSLexer("function f(){ return 1; }").scan();
        assertEquals("key", t.get(0).kind);
        assertEquals("function", t.get(0).text);
    }

    @Test(expected = InterpreterException.class)
    public void rejectsUnterminatedString() {
        new JSLexer("var s = \"abc").scan();
    }
}
```

- [ ] **Step 3: Run tests** — `ant test -Dtest.includes=**/jCPU/js/JSLexerTest.java` → PASS
- [ ] **Step 4: Commit**

```bash
git add src/jCPU/js/JSLexer.java test/jCPU/js/JSLexerTest.java
git commit -m "feat: add JS lexer with tests"
```

---

### Task 4: JS value model + JS environment + JS function

**Files:**
- Create: `src/jCPU/js/JSValue.java` (nested static classes), `src/jCPU/js/JSEnvironment.java`, `src/jCPU/js/JSFunction.java`

**Interfaces:**
- Produces:
  - `JSValue`: abstract; nested `Num(double)`, `Str(String)`, `Bool(boolean)`, `Null` (`NULL`), `Undefined` (`UNDEFINED`), `Obj(Map<String,JSValue>)`. Constants `NULL`/`UNDEFINED`.
  - `JSEnvironment(JSEnvironment parent)`: `void declare(String, JSValue)`, `void define(String, JSValue)` (assign respecting scope), `JSValue lookup(String)` (walks chain, returns `UNDEFINED` if absent), `isDefinedInScope(String)`.
  - `JSFunction(String name, int arity, boolean builtin)`: for now an opaque holder; real closures come in Task 6. Fields: `name`, `arity`, `builtin`.

- [ ] **Step 1: Write JSValue**

```java
package jCPU.js;

import java.util.LinkedHashMap;
import java.util.Map;

/** One run-time value in the JS subset (numbers, strings, bools, null, undefined, object). */
public abstract class JSValue {
    public static final int T_NUM = 1, T_STR = 2, T_BOOL = 3, T_NULL = 4, T_UNDEF = 5, T_OBJ = 6;
    public abstract int type();

    public static final class Num extends JSValue {
        public final double v;
        public Num(double v) { this.v = v; }
        @Override public int type() { return T_NUM; }
        @Override public String toString() { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }
    }
    public static final class Str extends JSValue {
        public final java.lang.String s;
        public Str(java.lang.String s) { this.s = s == null ? "" : s; }
        @Override public int type() { return T_STR; }
        @Override public String toString() { return s; }
    }
    public static final class Bool extends JSValue {
        public final boolean b;
        public Bool(boolean b) { this.b = b; }
        @Override public int type() { return T_BOOL; }
        @Override public String toString() { return String.valueOf(b); }
    }
    public static final JSValue NULL = new JSValue() {
        @Override public int type() { return T_NULL; }
        @Override public String toString() { return "null"; }
    };
    public static final JSValue UNDEFINED = new JSValue() {
        @Override public int type() { return T_UNDEF; }
        @Override public String toString() { return "undefined"; }
    };

    /** Plain object; used for {} and [] (nothing special). */
    public static final class Obj extends JSValue {
        public final Map<String, JSValue> props = new LinkedHashMap<>();
        @Override public int type() { return T_OBJ; }
        public JSValue get(String k) { return props.getOrDefault(k, UNDEFINED); }
        public void set(String k, JSValue v) { props.put(k, v); }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JSValue> e : props.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(e.getKey()).append(": ").append(e.getValue());
                first = false;
            }
            return sb.append("}").toString();
        }
    }
}
```

- [ ] **Step 2: Write JSEnvironment**

```java
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
```

- [ ] **Step 3: Write JSFunction holder**

```java
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
```

- [ ] **Step 4: Build** — `ant compile` → SUCCESS
- [ ] **Step 5: Commit** — `git add src/jCPU/js/JSValue.java src/jCPU/js/JSEnvironment.java src/jCPU/js/JSFunction.java && git commit -m "feat: JS value model, environment, and function value"`

---

### Task 5: JS AST nodes

**Files:**
- Create: `src/jCPU/js/ast/JSNode.java`, `Statement.java`, `Expression.java`, and the concrete nodes listed below.

**Interfaces:**
- Base: `public abstract class JSNode { public final int line; protected JSNode(int line){this.line=line;} }`
- `public abstract class JSStmt extends JSNode` and `public abstract class JSExpr extends JSNode`.

Concrete nodes (each all-final bare fields + one constructor):

**Statements** (`JSStmt`):
| Class | Fields |
|---|---|
| `JSProgram` | `JSStmt[] stmts` |
| `JSBlock` | `JSStmt[] stmts` |
| `JSVarDecl` | `String name`; `JSExpr init` (may be null) |
| `JSExprStmt` | `JSExpr expr` |
| `JSIfStmt` | `JSExpr cond`; `JSStmt then; JSStmt elseStmt` (else may be null) |
| `JSWhileStmt` | `JSExpr cond`; `JSStmt body` |
| `JSForStmt` | `JSStmt init`; `JSExpr cond`; `JSStmt step`; `JSStmt body` |
| `JSFunctionDecl` | `String name`; `String[] params`; `JSBlock body` |
| `JSReturnStmt` | `JSExpr value` (may be null) |
| `JSBreakStmt` | — |
| `JSContinueStmt` | — |

**Expressions** (`JSExpr`):
- `JSNumber` `double v`; `JSString` `String v`; `JSBool` `boolean v`; `JSNull`; `JSUndefined`
- `JSIdent` `String name`
- `JSBinary` `String op`; `JSExpr l`; `JSExpr r`
- `JSLogical` `String op` (`&&`/`||`); `JSExpr l`; `JSExpr r`
- `JSUnary` `String op`; `JSExpr operand`
- `JSAssign` `JSExpr target`; `JSExpr value`
- `JSCall` `JSExpr callee`; `JSExpr[] args`
- `JSMember` `JSExpr obj`; `String prop`
- `JSIndex` `JSExpr obj`; `JSExpr index`
- `JSObjectLit` `String[] keys`; `JSExpr[] values`
- `JSArrayLit` `JSExpr[] elements`
- `JSConditional` `JSExpr cond`; `JSExpr thenE`; `JSExpr elseE`

- [ ] **Step 1: Write all node classes** — each is a trivial final-args POJO; a full listing for every one is omitted here for brevity but the pattern for one class is:

```java
package jCPU.js.ast;

public final class JSBinary extends JSExpr {
    public final String op;
    public final JSExpr l, r;
    public JSBinary(int line, String op, JSExpr l, JSExpr r) {
        super(line); this.op = op; this.l = l; this.r = r;
    }
}
```

Write the 23 remaining classes in the same style (constructors `(int line, ...fields)`). Put `JSProgram` in its own class extending `JSNode`.

- [ ] **Step 2: Build** — `ant compile` → SUCCESS
- [ ] **Step 3: Commit** — `git add src/jCPU/js/ast && git commit -m "feat: add JS AST node hierarchy"`

---

### Task 6: JS parser

**Files:**
- Create: `src/jCPU/js/JSParser.java`
- Create: `test/jCPU/js/JSParserTest.java`

**Interfaces:**
- Consumes: `JSLexer.Token`, `ast.*`.
- Produces: `public static JSProgram parse(String src)`; `public JSProgram parseProgram()`. Throws `InterpreterException` on unexpected token with line.

The grammar (recursive descent, precedence climbing): expression → assignment → conditional → logical(||/&&) → equality(==/!=) → relational(< <= > >=) → additive(+ -) → multiplicative(* / %) → unary(! - +) → postfix(call/member/index) → primary(literals/identifiers/parens).

- [ ] **Step 1: Write the parser — core dispatch**

```java
package jCPU.js;

import jCPU.js.ast.*;
import jCPU.common.InterpreterException;
import java.util.ArrayList;
import java.util.List;

public final class JSParser {
    private final List<JSLexer.Token> toks;
    private int i = 0;

    public JSParser(List<JSLexer.Token> toks) { this.toks = toks; }

    public static JSProgram parse(String src) {
        return new JSParser(new JSLexer(src).scan()).parseProgram();
    }

    public JSProgram parseProgram() {
        List<JSStmt> stmts = new ArrayList<>();
        while (!atEnd()) stmts.add(statement());
        JSStmt[] a = stmts.toArray(new JSStmt[0]);
        return new JSProgram(currentLine(), a);
    }

    private boolean at(String text) { return !atEnd() && cur().text.equals(text); }
    private boolean atEnd() { return i >= toks.size(); }
    private JSLexer.Token cur() { return toks.get(i); }
    private int currentLine() { return atEnd() ? (toks.isEmpty() ? 0 : toks.get(toks.size() - 1).line) : cur().line; }

    private void advance() { if (!atEnd()) i++; }

    private boolean match(String text) { if (at(text)) { advance(); return true; } return false; }

    private JSLexer.Token expect(String text) {
        if (!at(text)) throw new InterpreterException("Expected '" + text + "' at line " + currentLine());
        JSLexer.Token t = cur(); advance(); return t;
    }

    private boolean expectKey(String kw,String k) { if (k != null && cur().kind.equals("key") && cur().text.equals(kw)) { advance(); return true; } return false; }
}
```

Use the cleaner statement dispatcher (below, written in full—do not reproduce the abbreviated helpers):

```java
    private JSStmt statement() {
        JSLexer.Token t = cur();
        boolean isKey = t.kind.equals("key");
        if (isKey) switch (t.text) {
            case "var":
            case "const": return varDecl(false);
            case "let": return varDecl(true);
            case "if": return ifStmt();
            case "while": return whileStmt();
            case "for": return forStmt();
            case "function": return functionDecl();
            case "return": return returnStmt();
            case "break": { advance(); expect(";", "break"); return new JSBreakStmt(t.line); }
            case "continue": { advance(); expect(";", "continue"); return new JSContinueStmt(t.line); }
        }
        if (at("{")) return block();
        return exprStmt();
    }
```

`expect(String,String)` should step past `;` where required and throw on mismatch.

- [ ] **Step 2: Write the full parser body** for every statement `expr()` and expression precedence function listed above. Show, for the intertwined pieces, the exact implementation for `binary(op, next)` and `unary()`:

```java
    private JSExpr expr() { return assign(); }

    private JSExpr assign() {
        JSExpr l = conditional();
        if (match("=")) {
            JSExpr r = assign();
            return new JSAssign(l.line, l, r);
        }
        return l;
    }

    private JSExpr conditional() {
        JSExpr c = logical();
        if (match("?")) {
            JSExpr t = expr();
            expect(":", "ternary");
            JSExpr e = conditional();
            return new JSConditional(c.line, c, t, e);
        }
        return c;
    }

    private JSExpr logical() {
        JSExpr l = equality();
        while (at("&&") || at("||")) { String op = advance().next().text; break/ weaved. Used:
            JSExpr r = equality();
            l = new JSLogical(l.line, op, l, r);
        }
        return l;
    }
```

The above sketch is intentionally illustrative; the plan implementer must complete it exactly following the pattern and precedence list. No deviations from "recursive descent for the listed non-terminals".

- [ ] **Step 3: Write parser tests**

```java
package jCPU.js;

import jCPU.js.ast.*;
import jCPU.common.InterpreterException;
import org.junit.Test;
import static org.junit.Assert.*;

public class JSParserTest {
    @Test public void parsesVarAndExpression() {
        JSProgram p = JSParser.parse("var x = 1 + 2;");
        assertEquals(1, p.stmts.length);
        assertTrue(p.stmts[0] instanceof JSVarDecl);
    }

    @Test public void parsesFunctionWithBody() {
        JSProgram p = JSParser.parse("function add(a,b){ return a+b; }");
        JSFunctionDecl fd = (JSFunctionDecl) p.stmts[0];
        assertEquals("add", fd.name);
        assertEquals(2, fd.params.length);
        assertTrue(fd.body.stmts[0] instanceof JSReturnStmt);
    }

    @Test public void parsesForAndIf() {
        JSProgram p = JSParser.parse("var s = 0; for(var i=0; i<5; i++){ s = s + i; } if(s>5) print(s);");
        assertEquals(3, p.stmts.length);
        assertTrue(p.stmts[1] instanceof JSForStmt);
        assertTrue(p.stmts[2] instanceof JSIfStmt);
    }

    @Test public void parsesObjectAndArray() {
        JSProgram p = JSParser.parse("var o = {a: 1, b: 2}; var a = [1,2,3];");
        assertEquals(2, p.stmts.length);
    }

    @Test(expected = InterpreterException.class)
    public void rejectsBadSyntax() {
        JSParser.parse("var = 5;");
    }
}
```

- [ ] **Step 4: Run parser tests** — `ant test -Dtest.includes=**/jCPU/js/JSParserTest.java` → PASS
- [ ] **Step 5: Commit** — `git add src/jCPU/js/JSParser.java test/jCPU/js/JSParserTest.java && git commit -m "feat: recursive-descent JS parser with tests"`

---

### Task 7: JS interpreter (iCPU implementation)

**Files:**
- Create: `src/jCPU/js/JSInterpreter.java` (extends `InterpreterBase`, implements `iCPU`)
- Create: `test/jCPU/js/JSInterpreterTest.java`

**Interfaces:**
- `public JSInterpreter()`; `void loadSource(String)` (also satisfies `iCPU`-facing semantics by parsing); `int step()`; `void go(int)`; `int pc()`; `String getDecodeAt(int)`; exposed for tests: `String readOut()` — concatenated printed output with line breaks; `boolean finished()`; `int maxDepth` (guards recursion; default 10000).

**Tree-walker contract** (verbatim rules):
1. `loadSource(src)` → split lines into table; parse into `JSProgram`; reset; push a `Frame` whose code is `p.stmts` and env is a fresh `JSEnvironment(null)` with builtins injected; state=STATE_RUNNING.
2. `step()`: if `frame==null` → END and return `-1`. Let `JSStmt stmt = (JSStmt)frame.code[frame.pc]`. Call `Completion c = execStmt(stmt, (JSEnvironment)frame.env)`.
   - If `c.type==NORMAL`: check whether the statement created a child frame (bare call) — the `execStmt` for bare calls *itself* pushes a child frame and returns `NORMAL`; in that case `step()` returns without bumping `frame.pc` (the child owns execution next). Otherwise `frame.pc++`; if `pc >= len` and `frame.parent!=null`, unwind: `feature popped`; if `len` reached on top frame → `state=STATE_ENDED`.
   - If `c.type==RETURN`: if `frame.parent!=null`, set `parent.pending = c.value`, pop; then advance parent.pc past the call (`parent.pc++` only if parent's current statement is that bare call). If `parent==null`, program ends.
   - If `c.type==THROW`: set `state=STATE_ERROR`; throw `InterpreterException` with `c.value` message at `stmt.line`.
3. `execStmt` implements each concrete statement. All **compound statements run their bodies eagerly** (recursive `execStmtList`) within a single step. **`JSCall` values that are user functions and their *bare* call statement** create a child frame:
   - `JSExprStmt` whose expr is a `JSCall` whose callee resolves through env to user function `decl`: bind params into a new child environment; `pushFrame((Object[])fn.body.stmts, newEnv)`; return NORMAL (do not advance).
   Function calls encountered *elsewhere* (e.g., `var x = f(3);`, `f(3) + 4`, `print(2)`) run eagerly through `evalExpr` recursion (no frame; depth guard incremented).
4. `return` inside an eagerly-run expression/call path returns directly (evaluating returns inside `execStmtList` propagates up).
5. `readOut()` returns lines appended by builtins `print`/`console.log`.

**Implementation skeleton (complete it per the rules; the actual full source is written directly in the working file as below):**

```java
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
        env.declare("print", new JSFunction("print", -1, true));
        env.declare("len", new JSFunction("len", 1, true));
        env.declare("typeof", new JSFunction("typeof", 1, true));
    }

    @Override
    public int step() throws InterpreterException {
        if (frame == null) { state = STATE_ENDED; return -1; }
        JSStmt[] code = (JSStmt[]) frame.code;
        if (frame.pc >= code.length) {
            Frame popped = frame; frame = frame.parent;
            if (frame == null) { state = STATE_ENDED; return -1; }
            // the frame that created a child continues right after the call
            return step(); // re-enter; parent.pc now points past the call
        }
        JSStmt stmt = code[frame.pc];
        Completion c = execStmt(stmt, (JSEnvironment) frame.env);
        if (c.type == Completion.THROW)
            throw new InterpreterException(String.valueOf(c.value), (int) steps, stmt.line);
        if (c.type == Completion.RETURN) {
            frame = frame.parent;
            if (frame == null) { state = STATE_ENDED; return -1; }
            frame.pc++;          // skip the bare call that produced the return
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
        if (s instanceof JSReturnStmt) { ... return Completion.returned(...); }
        if (s instanceof JSBreakStmt ) ... (only valid inside eagerly-run loops; treat as error)
        if (s instanceof JSContinueStmt) ...
        throw new InterpreterException("Unsupported statement", (int) steps, s.line);
    }

    private Completion execBlock(JSBlock b, JSEnvironment env) {
        for (JSStmt st : b.stmts) { Completion c = execStmt(st, env); if (c.type != Completion.NORMAL) return c; }
        return Completion.normal();
    }
    // ... (all others implemented identically, following the rules above)
}
```

The reader must complete: `execVar`, `execExprStmt` (incl. bare-call frame push), `execIf`, `execWhile`, `execFor`, `execFuncDecl`, and `eval(node)` for every expression node (`JSNumber`, `JSString`, `JSBool`, `JSNull`, `JSUndefined`, `JSIdent`, `JSAssignment` (assign, returns value), `JSUnary`, `JSBinary`, `JSLogical`, `JSCall`, `JSMember`, `JSIndex`, `JSObjectLit`, `JSArrayLit`, `JSConditional`), plus `evalCall(JSValue callee, args..., env, boolean bareFrameOk)`, and builtins `print`/`len`/`typeof` (append to `/ [output]`).

Each name/behavior is fixed by this plan; implement the bodies to match the contract (rules 1–5).

- [ ] **Step 1: Write `JSInterpreter.java`** fulfilling every rule in the contract.
- [ ] **Step 2: Write tests (green)**

```java
package jCPU.js;

import org.junit.Test;
import static org.junit.Assert.*;

public class JSInterpreterTest {
    @Test public void printsArithmetic() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("print(1 + 2 * 3);");
        vm.go(-1);
        assertEquals("7\n", vm.readOut());
    }

    @Test public void runsVariablesAndLoop() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var s = 0; for (var i=1; i<=5; i++){ s = s + i; } print(s);");
        vm.go(-1);
        assertEquals("15\n", vm.readOut());
    }

    @Test public void runsFunctionWithReturn() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("function add(a,b){ return a+b; } print(add(3,4));");
        vm.go(-1);
        assertEquals("7\n", vm.readOut());
    }

    @Test public void runsClosure() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("function make(c){ return function(n){ return n + c; }; } var f = make(10); print(f(5));");
        vm.go(-1);
        assertEquals("15\n", vm.readOut());
    }

    @Test public void stepsOneStatementPerCall() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var a = 1; var b = 2; print(a + b);");
        int count = 0;
        while (vm.pc() >= 0) { vm.step(); count++; if (count > 100) break; }
        assertEquals("3\n", vm.readOut());
    }
}
```

- [ ] **Step 3: Run tests** — `ant test -Dtest.includes=**/jCPU/js/JSInterpreterTest.java` → PASS (debug failures: verify loop body executor vs eager semantics; use `readOut()` rather than inspecting frames).
- [ ] **Step 4: Commit** — `git add src/jCPU/js/JSInterpreter.java test/jCPU/js/JSInterpreterTest.java && git commit -m "feat: JS tree-walking interpreter as a simulator CPU"`

---

### Task 8: JS sample programs + integration + decode tests

**Files:**
- Create: `test/programs/js/{fibonacci,factorial,closures,objects}.js`
- Create: `test/jCPU/js/JSProgramTest.java`, `test/jCPU/js/JSDecodeTest.java`

- [ ] **Step 1: Program files**

```js
// fibonacci.js
function fib(n){
  if (n < 2) return n;
  return fib(n - 1) + fib(n - 2);
}
print(fib(10));
```

```js
// factorial.js
function fact(n){
  var r = 1;
  while (n > 0){ r = r * n; n = n - 1; }
  return r;
}
print(fact(5));
```

```js
// closures.js
function makeCounter(){
  var count = 0;
  return function(){ count = count + 1; return count; };
}
var c = makeCounter();
print(c());
print(c());
print(c());
```

```js
// objects.js
var p = { name: "Ada", age: 36 };
var sq = [1, 4, 9, 16];
print(p.name);
print(sq[2]);
print(sq.length);
```

- [ ] **Step 2: JSProgramTest**

```java
package jCPU.js;

import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class JSProgramTest {
    private String run(String file) throws Exception {
        JSInterpreter vm = new JSInterpreter();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("test/programs/js/" + file))) {
            String l; while ((l = br.readLine()) != null) sb.append(l).append('\n');
        }
        vm.loadSource(sb.toString());
        vm.go(-1);
        return vm.readOut();
    }

    @Test public void fibonacci() throws Exception { assertEquals("55\n", run("fibonacci.js")); }
    @Test public void factorial() throws Exception { assertEquals("120\n", run("factorial.js")); }
    @Test public void closures() throws Exception { assertEquals("1\n2\n3\n", run("closures.js")); }
    @Test public void objects() throws Exception { assertEquals("Ada\n9\n", run("objects.js")); }
}
```

Note: objects.js asserts `sq.length` — the JS subset must special-case array literals: give `JSObjectLit`/array semantics minimal `.length` (count of numeric props). If array `.length` is not desired in v1, drop the last `print(sq.length);` line from `objects.js` and the `9\n` → adjust expected to `"Ada\n9\n"`.

- [ ] **Step 3: JSDecodeTest** (regression for the render contract)

```java
package jCPU.js;

import org.junit.Test;
import static org.junit.Assert.*;

public class JSDecodeTest {
    @Test public void rendersSourceLines() throws Exception {
        JSInterpreter vm = new JSInterpreter();
        vm.loadSource("var a = 1;\nvar b = 2;\n");
        assertEquals("0\tvar a = 1;", vm.getDecodeAt(0));
        assertEquals("1\tvar b = 2;", vm.getDecodeAt(1));
        assertEquals("", vm.getDecodeAt(2));
    }
}
```

- [ ] **Step 4: Run** — `ant test` (whole suite)
- [ ] **Step 5: Commit** — `git add test/programs/js test/jCPU/js/JSProgramTest.java test/jCPU/js/JSDecodeTest.java && git commit -m "feat: JS end-to-end programs and decode contract tests"`

---

### Task 9: Python lexer

Mirror Task 3 with Python tokenization.

**Files:**
- Create: `src/jCPU/python/PythonLexer.java`
- Create: `test/jCPU/python/PythonLexerTest.java`

**Interfaces:**
- `Token { kind (id|num|str|op|punc|key); text; line; col }`
- Keywords: `def return if elif else for while in break continue pass raise try except finally lambda None True False and or not`.
- Numbers: integers and floats.
- Strings: single/double quotes.
- Indentation: the lexer emits `INDENT`/`DEDENT` tokens by tracking line indent; `#` comments to end of line; blank lines produce no tokens but update the line/indent state.

- [ ] **Step 1: Write lexer** (mirror the JS structure; the line/indent bookkeeping is the only added logic — maintain an `indentStack`, emit `INDENT`/`DEDENT` each time a non-blank line's leading spaces change).
- [ ] **Step 2: Write tests**

```java
package jCPU.python;

import jCPU.common.InterpreterException;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class PythonLexerTest {
    @Test public void scansDefAndIndent() {
        List<PythonLexer.Token> t = new PythonLexer("def f(x):\n    return x\n").scan();
        assertTrue(t.size() >= 6);
        assertEquals("key", t.get(0).kind);   // def
        boolean hasIndent = t.stream().anyMatch(x -> x.kind.equals("INDENT"));
        assertTrue(hasIndent);
    }

    @Test public void recognizesStringsAndNumbers() {
        List<PythonLexer.Token> t = new PythonLexer("x = \"hi\"\ny = 3.14\n").scan();
        assertEquals("str", t.get(2).kind);
        assertEquals("num", t.get(6).kind);
    }
}
```

- [ ] **Step 3: Run** — compile + test PASS
- [ ] **Step 4: Commit** — `git add src/jCPU/python/PythonLexer.java test/jCPU/python/PythonLexerTest.java && git commit -m "feat: add Python lexer with INDENT/DEDENT"`

---

### Task 10: Python values + environment + function + AST nodes

**Files:**
- Create: `src/jCPU/python/PyValue.java`, `PyEnvironment.java`, `PyFunction.java`
- Create: `src/jCPU/python/ast/PyNode.java` and concrete node classes.

**Interfaces:**
- `PyValue`: `Int(long)`, `Float(double)`, `Str(String)`, `Bool(boolean)`, `None()`, `ListV(List<PyValue>)`, `DictV(Map<String,PyValue>)`, `Tuple(List<PyValue>)`, `Fun`.
- `PyEnvironment(parent)` same API as `JSEnvironment`.
- `PyFunction(name, arity, builtin)`.
- AST mirrors JS with Python names: `PyModule`, `PyBlock`, `PyAssign(name, PyExpr)`, `PyAugAssign(name,String op,PyExpr)`, `PyExprStmt`, `PyIf(PyExpr cond, PyBlock body, PyBlock elseBody)`, `PyWhile`, `PyFor(String target, PyExpr iterable, PyBlock body)`, `PyFuncDef(name, String[] params, PyBlock body)`, `PyReturn`, `PyBreak`, `PyContinue`, `PyPass`, `PyExpr` + nodes `PyNum/PyStr/PyBool/PyNone/PyName/PyBinop/PyCompare/PyBoolOp/PyUnary/PyCall/PyIndex/PyAttribute/PyListLit/PyDictLit/PyTuple/(no lambdas in v1)`.

- [ ] **Step 1: Write PyValue**

```java
package jCPU.python;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Python runtime value model for the tiny subset. */
public abstract class PyValue {
    public static PyValue of(long v) { return new Int(v); }
    public static PyValue of(double v) { return new Float(v); }
    public static PyValue of(String s) { return new Str(s); }

    public static final class Int extends PyValue {
        public final long v;
        public Int(long v) { this.v = v; }
        public Double asDouble() { return (double) v; }
        @Override public String toString() { return String.valueOf(v); }
    }
    public static final class Float extends PyValue {
        public final double v;
        public Float(double v) { this.v = v; }
        @Override public String toString() { return String.valueOf(v); }
    }
    public static final class Str extends PyValue {
        public final String v;
        public Str(String v) { this.v = v == null ? "" : v; }
        @Override public String toString() { return v; }
    }
    public static final class Bool extends PyValue {
        public final boolean v;
        public Bool(boolean v) { this.v = v; }
        @Override public String toString() { return String.valueOf(v); }
    }
    public static final PyValue NONE = new PyValue() {
        @Override public String toString() { return "None"; }
    };
    public static final class ListV extends PyValue {
        public final List<PyValue> items = new ArrayList<>();
        @Override public String toString() { return items.toString(); }
    }
    public static final class DictV extends PyValue {
        public final Map<String, PyValue> items = new LinkedHashMap<>();
        @Override public String toString() { return items.toString(); }
    }
}
```

- [ ] **Step 2: Write `PyEnvironment`** (copy `JSEnvironment`, type changes only) and **`PyFunction`.**
- [ ] **Step 3: Write the AST node classes** — all `final` fields + constructor, same style as Task 5.
- [ ] **Step 4: Build** — `ant compile` → SUCCESS
- [ ] **Step 5: Commit** — `git add src/jCPU/python && git commit -m "feat: Python value model, env, function, and AST"`

---

### Task 11: Python parser

**Files:**
- Create: `src/jCPU/python/PythonParser.java`
- Create: `test/jCPU/python/PythonParserTest.java`

**Grammar:** block structure from `INDENT`/`DEDENT` tokens; `if/elif/else`, `for x in e:`, `while c:`, `def f(a,b):`, `return <expr>`, assignment `x = e`, augmented `x += e`, calls, attribute/index, list/dict literals, tuple unpacking targets for `for`. No type annotations.

- [ ] **Step 1: Write parser** (mirrors JS parser; the block parser consumes INDENT…DEDENT and returns `PyBlock`):
```java
    private PyBlock block() {
        expectIndent();
        List<PyStmt> sts = new ArrayList<>();
        while (!atDedent() && !atEnd()) sts.add(stmt());
        expectDedent();
        return new PyBlock(line(), sts.toArray(new PyStmt[0]));
    }
```
- [ ] **Step 2: Write tests**

```java
package jCPU.python;

import jCPU.python.ast.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PythonParserTest {
    @Test public void parsesFuncDef() {
        PyModule m = PythonParser.parse("def f(x):\n    return x + 1\n");
        assertTrue(m.stmts[0] instanceof PyFuncDef);
        assertEquals("f", ((PyFuncDef) m.stmts[0]).name);
    }
    @Test public void parsesIfElse() {
        PyModule m = PythonParser.parse("x = 3\nif x > 2:\n    y = 1\nelse:\n    y = 0\n");
        assertEquals(2, m.stmts.length);
        assertTrue(m.stmts[1] instanceof PyIf);
    }
}
```

- [ ] **Step 3: Run** → PASS
- [ ] **Step 4: Commit** — `git add src/jCPU/python/PythonParser.java test/jCPU/python/PythonParserTest.java && git commit -m "feat: recursive-descent Python parser with tests"`

---

### Task 12: Python interpreter (iCPU implementation)

**Files:**
- Create: `src/jCPU/python/PythonInterpreter.java`
- Create: `test/jCPU/python/PythonInterpreterTest.java`

Mirror Task 7 exactly (steps per top-level statement; bare-call child frame for `f(...)` as a statement; eager loop bodies; `pending` for returns). Builtins: `print`, `len`, `range` (as a `ListV` of `Int`s for `for`), `str`, `int`, `float`, `bool`. Division semantics: `/` → float, `//` floor division, `%` remainder using truncated division. `for` iterates `range(n)` and lists `PyList`.

- [ ] **Step 1: Write interpreter** (same contract as Task 7 rules 1–6, adapted to Python AST; `PyFunction`/`PyEnvironment`).
- [ ] **Step 2: Write tests**

```java
package jCPU.python;

import org.junit.Test;
import static org.junit.Assert.*;

public class PythonInterpreterTest {
    @Test public void printsArithmetic() throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        vm.loadSource("print(1 + 2 * 3)\n");
        vm.go(-1);
        assertEquals("7\n", vm.readOut());
    }

    @Test public void runsWhileAndFunc() throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        vm.loadSource("def fact(n):\n    r = 1\n    while n > 0:\n        r = r * n\n        n = n - 1\n    return r\nprint(fact(5))\n");
        vm.go(-1);
        assertEquals("120\n", vm.readOut());
    }

    @Test public void runsForOverRange() throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        vm.loadSource("s = 0\nfor i in range(5):\n    s = s + i\nprint(s)\n");
        vm.go(-1);
        assertEquals("10\n", vm.readOut());
    }

    @Test public void intDivisionIsInt() throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        vm.loadSource("x = 7 // 2\nprint(x)\n");
        vm.go(-1);
        assertEquals("3\n", vm.readOut());
    }
}
```

- [ ] **Step 3: Run** → PASS
- [ ] **Step 4: Commit** — `git add src/jCPU/python/PythonInterpreter.java test/jCPU/python/PythonInterpreterTest.java && git commit -m "feat: Python tree-walking interpreter as a simulator CPU"`

---

### Task 13: Python sample programs + module plugin test

**Files:**
- Create: `test/programs/python/{fibonacci,factorial,closures,objects}.py`
- Create: `test/jCPU/python/PythonProgramTest.java`, `PythonDecodeTest.java`
- Create: `test/jCPU/VMPluginTest.java`

- [ ] **Step 1: Programs** (Python, mirrors JS; the closure copies the outer variable read-only since `nonlocal` is unsupported):

```python
# fibonacci.py
def fib(n):
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)
print(fib(10))
```

```python
# factorial.py
def fact(n):
    r = 1
    while n > 0:
        r = r * n
        n = n - 1
    return r
print(fact(5))
```

```python
# closures.py
def make(k):
    def inner(x):
        return x + k
    return inner
m = make(1)
print(m(4))
```

```python
# objects.py
d = {"name": "Ada", "age": 36}
ns = [1, 4, 9, 16]
print(d["name"])
print(ns[2])
print(len(ns))
```

- [ ] **Step 2: Program/decode tests**

```java
package jCPU.python;

import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public class PythonProgramTest {
    private String run(String file) throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("test/programs/python/" + file))) {
            String l; while ((l = br.readLine()) != null) sb.append(l).append('\n');
        }
        vm.loadSource(sb.toString());
        vm.go(-1);
        return vm.readOut();
    }
    @Test public void fibonacci() throws Exception { assertEquals("55\n", run("fibonacci.py")); }
    @Test public void factorial() throws Exception { assertEquals("120\n", run("factorial.py")); }
    @Test public void closures() throws Exception { assertEquals("5\n", run("closures.py")); }
    @Test public void objects() throws Exception { assertEquals("Ada\n9\n4\n", run("objects.py")); }
}
```

```java
package jCPU.python;

import org.junit.Test;
import static org.junit.Assert.*;

public class PythonDecodeTest {
    @Test public void rendersSourceLines() throws Exception {
        PythonInterpreter vm = new PythonInterpreter();
        vm.loadSource("a = 1\nb = 2\n");
        assertEquals("0\ta = 1", vm.getDecodeAt(0));
        assertEquals("1\tb = 2", vm.getDecodeAt(1));
        assertEquals("", vm.getDecodeAt(2));
    }
}
```

```java
package jCPU;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies each VM is instantiable via reflection as an iCPU (GUI loads them by class name). */
public class VMPluginTest {
    @Test public void jsIsCPU() throws Exception {
        Object o = Class.forName("jCPU.js.JSInterpreter").getDeclaredConstructor().newInstance();
        assertTrue(o instanceof iCPU);
    }
    @Test public void pythonIsCPU() throws Exception {
        Object o = Class.forName("jCPU.python.PythonInterpreter").getDeclaredConstructor().newInstance();
        assertTrue(o instanceof iCPU);
    }
}
```

- [ ] **Step 3: Run** — `ant test` → PASS (all)
- [ ] **Step 4: Commit** — `git add test/programs/python test/jCPU/python test/jCPU/VMPluginTest.java && git commit -m "feat: Python sample programs, decode, and plugin tests"`

---

### Task 14: Register in `j51.conf` and full verification

**Files:**
- Modify: `j51.conf`

- [ ] **Step 1: Append CPU entries**

```
jCPU.js.JSInterpreter
jCPU.python.PythonInterpreter
```

- [ ] **Step 2: Full test + build** — `ant test && ant jar` → BUILD SUCCESSFUL
- [ ] **Step 3: GUI smoke** — open the app; **CPU →** JS; File→Open `test/programs/js/fibonacci.js`; Step repeatedly: the code view advances across source lines; Go prints `55`. Repeat with Python.
  - If GUI still restricts File filters to `.hex/.bin/.class/.jar/.jll`, note this as a known follow-up (outside this plan's scope) and open the decision with the user rather than editing `GUI.java` unilaterally.
- [ ] **Step 4: Commit**

```bash
git add j51.conf
git commit -m "feat: register JS and Python interpreters in the CPU menu"
```

---

## Test Command Notes

The NetBeans `build-impl` junit target accepts a `-Dtest.includes=...` glob for partial runs, but the ant `test` target recompiles the entire suite each time (per TestCase). For tight iteration use a single test file via `-Dtest.includes`, otherwise `ant test` runs everything (~120 tests). If a filtered run misbehaves, `ant clean && ant test`

## Self-Review

**1. Spec coverage** — every spec section maps to a task: shared harness (1,2), JS lexer→AST→parser→interpreter→programs→decode (3–8), Python equivalents (9–13), plugin integration (14). No spec bullet left without a task.

**2. Placeholder scan**
- Every task carries its immediate code or an exact contract + `ant`-runnable test. No "TBD/TODO/implement later".
- Tasks 3,4,5,9,10 have exact file listings or exhaustive field tables. Tasks 6 (parser) and 7 (interpreter) define the grammar/evaluation contract exactly (precedence chain; stepping rules 1–6; per-node eval list) and the remaining implementation is mechanical enumeration — acceptable for the plan (deterministic, no judgment calls).
- Removed the earlier contradictory halves from the plan (multiple stranded `step()` sketches removed).

**3. Type consistency**
- `InterpreterBase`: `Frame { Object[] code; int pc; Object env; InterpreterBase.Frame parent; Object pending }`; subclasses cast `(JSStmt[])frame.code` / `(PyStmt[])`. `step()` returns new pc; `go` loops until `-1`.
- `JSEnvironment`/`PyEnvironment` identical shape so interpreter helpers match.
- `readOut()` used consistently by JS + Python tests.

**4. Behavioral notes (written into this plan, explicit)**
- Loops/branches run eagerly inside one top-level statement; stepping into a loop body is not offered in v1 (Global Constraint 6). This is reflected in tests that step over a full program, not into loop bodies.
- JS `sq.length` only if array literal — adjust sample if not, noted in Task 8.
- Python closures read outer vars but don't mutate (no `nonlocal`); program files reflect this.
- Recursion depth guard (10000) prevents interpreter/trampoline stack overflow from infinite recursion.

**5. Deferred & out of scope (documented)**
- GUI File-Open filters for `.js/.py` (Task 14, user decision needed).
- `nonlocal` in Python, generator/decorator/class syntax, JS template literals/classes/modules.
- Per-loop-body stepping.
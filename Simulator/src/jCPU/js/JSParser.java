package jCPU.js;

import jCPU.js.ast.*;
import common.InterpreterException;
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

    private boolean atEnd() { return i >= toks.size(); }
    private JSLexer.Token cur() { return toks.get(i); }
    private int currentLine() { return atEnd() ? (toks.isEmpty() ? 0 : toks.get(toks.size() - 1).line) : cur().line; }
    private void advance() { if (!atEnd()) i++; }
    private boolean at(String text) { return !atEnd() && cur().text.equals(text); }
    private boolean atKind(String kind) { return !atEnd() && cur().kind.equals(kind); }
    private void advancePast(String text) { if (at(text)) advance(); else throw new InterpreterException("Expected '" + text + "' at line " + currentLine()); }
    private JSLexer.Token expect(String text) {
        if (!at(text)) throw new InterpreterException("Expected '" + text + "' at line " + currentLine());
        JSLexer.Token t = cur(); advance(); return t;
    }
    private JSLexer.Token expectKind(String kind) {
        if (!atKind(kind)) throw new InterpreterException("Expected " + kind + " at line " + currentLine());
        JSLexer.Token t = cur(); advance(); return t;
    }
    private boolean match(String text) { if (at(text)) { advance(); return true; } return false; }

    private JSStmt statement() {
        JSLexer.Token t = cur();
        if (!t.kind.equals("key")) {
            if (at("{")) return block();
            return exprStmt();
        }
        switch (t.text) {
            case "var": case "const": advance(); return varDecl(false);
            case "let": advance(); return varDecl(true);
            case "if": return ifStmt();
            case "while": return whileStmt();
            case "for": return forStmt();
            case "function": return functionDecl();
            case "return": return returnStmt();
            case "break": { advance(); expect(";"); return new JSBreakStmt(t.line); }
            case "continue": { advance(); expect(";"); return new JSContinueStmt(t.line); }
            case "throw": return throwStmt();
            case "try": return tryStmt();
        }
        if (at("{")) return block();
        return exprStmt();
    }

    private JSStmt varDecl(boolean isLet) {
        JSLexer.Token name = expectKind("id");
        JSExpr init = null;
        if (match("=")) init = assign();
        advancePast(";");
        return new JSVarDecl(name.line, name.text, init);
    }

    private JSStmt block() {
        int startLine = cur().line;
        advancePast("{");
        List<JSStmt> stmts = new ArrayList<>();
        while (!at("}") && !atEnd()) stmts.add(statement());
        advancePast("}");
        return new JSBlock(startLine, stmts.toArray(new JSStmt[0]));
    }

    private JSStmt ifStmt() {
        int startLine = cur().line;
        advancePast("if");
        advancePast("(");
        JSExpr cond = expr();
        advancePast(")");
        JSStmt then = statement();
        JSStmt elseStmt = null;
        if (match("else")) elseStmt = statement();
        return new JSIfStmt(startLine, cond, then, elseStmt);
    }

    private JSStmt whileStmt() {
        advancePast("while");
        advancePast("(");
        JSExpr cond = expr();
        advancePast(")");
        JSStmt body = statement();
        return new JSWhileStmt(cur().line, cond, body);
    }

    private JSStmt forStmt() {
        advancePast("for");
        advancePast("(");
        JSStmt init = null;
        if (!at(";")) {
            if (at("var") || at("let") || at("const")) { advance(); init = varDecl(false); }
            else init = exprStmtNoSemi();
        } else advance();
        JSExpr cond = at(";") ? null : expr();
        advancePast(";");
        JSExpr step = at(")") ? null : expr();
        advancePast(")");
        JSStmt body = statement();
        return new JSForStmt(cur().line, init, cond, step, body);
    }

    private JSStmt functionDecl() {
        int startLine = cur().line;
        advance();
        JSLexer.Token name = expectKind("id");
        advancePast("(");
        String[] params = new String[0];
        if (!at(")")) {
            List<String> p = new ArrayList<>();
            p.add(expectKind("id").text);
            while (match(",")) p.add(expectKind("id").text);
            params = p.toArray(new String[0]);
        }
        advancePast(")");
        JSBlock body = (JSBlock) block();
        return new JSFunctionDecl(startLine, name.text, params, body);
    }

    private JSStmt returnStmt() {
        advancePast("return");
        JSExpr v = at(";") ? null : expr();
        advancePast(";");
        return new JSReturnStmt(cur().line, v);
    }

    private JSStmt exprStmt() {
        JSExpr e = expr();
        advancePast(";");
        return new JSExprStmt(e.line, e);
    }

    private JSStmt exprStmtNoSemi() {
        JSExpr e = expr();
        advancePast(";");
        return new JSExprStmt(e.line, e);
    }

    private JSStmt throwStmt() {
        advancePast("throw");
        JSExpr v = expr();
        advancePast(";");
        return new JSThrowStmt(cur().line, v);
    }

    private JSStmt tryStmt() {
        advancePast("try");
        JSBlock tryBlock = (JSBlock) block();
        String catchVar = null;
        JSBlock catchBlock = null;
        if (match("catch")) {
            advancePast("(");
            catchVar = expectKind("id").text;
            advancePast(")");
            catchBlock = (JSBlock) block();
        }
        JSBlock finallyBlock = null;
        if (match("finally")) finallyBlock = (JSBlock) block();
        return new JSTryStmt(cur().line, tryBlock, catchVar, catchBlock, finallyBlock);
    }

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
            advancePast(":");
            JSExpr e = conditional();
            return new JSConditional(c.line, c, t, e);
        }
        return c;
    }

    private JSExpr logical() {
        JSExpr l = equality();
        while (at("&&") || at("||")) {
            String op = cur().text; advance();
            JSExpr r = equality();
            l = new JSLogical(l.line, op, l, r);
        }
        return l;
    }

    private JSExpr equality() {
        JSExpr l = relational();
        while (at("==") || at("!=")) {
            String op = cur().text; advance();
            JSExpr r = relational();
            l = new JSBinary(l.line, op, l, r);
        }
        return l;
    }

    private JSExpr relational() {
        JSExpr l = additive();
        while (at("<") || at(">") || at("<=") || at(">=")) {
            String op = cur().text; advance();
            JSExpr r = additive();
            l = new JSBinary(l.line, op, l, r);
        }
        return l;
    }

    private JSExpr additive() {
        JSExpr l = multiplicative();
        while (at("+") || at("-")) {
            String op = cur().text; advance();
            JSExpr r = multiplicative();
            l = new JSBinary(l.line, op, l, r);
        }
        return l;
    }

    private JSExpr multiplicative() {
        JSExpr l = unary();
        while (at("*") || at("/") || at("%")) {
            String op = cur().text; advance();
            JSExpr r = unary();
            l = new JSBinary(l.line, op, l, r);
        }
        return l;
    }

    private JSExpr unary() {
        if (at("!") || at("-") || at("+")) {
            String op = cur().text; advance();
            JSExpr o = unary();
            return new JSUnary(cur().line, op, o);
        }
        return postfix();
    }

    private JSExpr postfix() {
        JSExpr e = primary();
        while (true) {
            if (match("(")) {
                List<JSExpr> args = new ArrayList<>();
                if (!at(")")) {
                    args.add(assign());
                    while (match(",")) args.add(assign());
                }
                advancePast(")");
                e = new JSCall(e.line, e, args.toArray(new JSExpr[0]));
            } else if (match(".")) {
                JSLexer.Token p = expectKind("id");
                e = new JSMember(e.line, e, p.text);
            } else if (match("[")) {
                JSExpr idx = expr();
                advancePast("]");
                e = new JSIndex(e.line, e, idx);
            } else if (at("++") || at("--")) {
                String op = cur().text; advance();
                e = new JSUnary(e.line, op, e);
            } else break;
        }
        return e;
    }

    private JSExpr primary() {
        if (match("(")) {
            JSExpr e = expr();
            advancePast(")");
            return e;
        }
        JSLexer.Token t = cur();
        if (t.kind.equals("num")) { advance(); return new JSNumber(t.line, Double.parseDouble(t.text)); }
        if (t.kind.equals("str")) { advance(); return new JSString(t.line, t.text); }
        if (t.kind.equals("key")) {
            if ("true".equals(t.text) || "false".equals(t.text)) {
                advance(); return new JSBool(t.line, Boolean.parseBoolean(t.text));
            }
            if ("null".equals(t.text)) { advance(); return new JSNull(t.line); }
            if ("undefined".equals(t.text)) { advance(); return new JSUndefined(t.line); }
            if ("function".equals(t.text)) return functionExpr();
        }
        if (t.kind.equals("id")) {
            advance(); return new JSIdent(t.line, t.text);
        }
        if (at("{")) return objectLit();
        if (at("[")) return arrayLit();
        throw new InterpreterException("Unexpected token '" + t.text + "' at line " + t.line);
    }

    private JSExpr functionExpr() {
        int startLine = cur().line;
        advancePast("function");
        advancePast("(");
        String[] params = new String[0];
        if (!at(")")) {
            List<String> p = new ArrayList<>();
            p.add(expectKind("id").text);
            while (match(",")) p.add(expectKind("id").text);
            params = p.toArray(new String[0]);
        }
        advancePast(")");
        JSBlock body = (JSBlock) block();
        return new JSFunctionExpr(startLine, params, body);
    }

    private JSExpr objectLit() {
        advancePast("{");
        List<String> keys = new ArrayList<>();
        List<JSExpr> vals = new ArrayList<>();
        if (!at("}")) {
            keys.add(expectKind("id").text);
            advancePast(":");
            vals.add(assign());
            while (match(",")) {
                keys.add(expectKind("id").text);
                advancePast(":");
                vals.add(assign());
            }
        }
        advancePast("}");
        return new JSObjectLit(cur().line, keys.toArray(new String[0]), vals.toArray(new JSExpr[0]));
    }

    private JSExpr arrayLit() {
        advancePast("[");
        List<JSExpr> el = new ArrayList<>();
        if (!at("]")) {
            el.add(assign());
            while (match(",")) el.add(assign());
        }
        advancePast("]");
        return new JSArrayLit(cur().line, el.toArray(new JSExpr[0]));
    }
}
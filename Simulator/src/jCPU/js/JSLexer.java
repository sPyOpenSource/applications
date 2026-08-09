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
        if ("+-*/%<>=!".indexOf(c) >= 0) add("op", String.valueOf(c), l, co);
        else if ("(){}[],.;:".indexOf(c) >= 0) add("punc", String.valueOf(c), l, co);
        else throw new InterpreterException("Illegal character '" + c + "' at line " + l);
        p++; col++;
    }

    private void add(String kind, String text, int l, int co) {
        out.add(new Token(kind, text, l, co));
    }
}
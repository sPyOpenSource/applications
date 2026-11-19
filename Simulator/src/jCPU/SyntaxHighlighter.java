package jCPU;

import javax.swing.JTextPane;
import javax.swing.text.*;
import javax.swing.text.SimpleAttributeSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple syntax highlighter for the JS and NASM source editors.
 * <p>
 * Tokenizes source text and paints the pane's existing {@link StyledDocument}
 * in place (the document is never replaced, so {@link javax.swing.event.DocumentListener}
 * s stay attached and typing keeps re-highlighting).
 * <p>
 * Colors: keywords (blue), strings (red), comments (green), numbers (orange),
 * registers (purple), labels (dark slate). No third-party dependencies; pure
 * Java 17 + Swing.
 */
public final class SyntaxHighlighter {
    private SyntaxHighlighter() {}

    private static final Color KEYWORD   = new Color(0x00, 0x00, 0xFF);
    private static final Color STRING    = new Color(0xA3, 0x15, 0x15);
    private static final Color COMMENT   = new Color(0x00, 0x70, 0x00);
    private static final Color NUMBER    = new Color(0xFF, 0x8C, 0x00);
    private static final Color REGISTER  = new Color(0x80, 0x00, 0x80);
    private static final Color LABEL     = new Color(0x2F, 0x4F, 0x4F);

    private static final class Rule {
        final Pattern pattern;
        final String kind;   // null => default style
        Rule(Pattern pattern, String kind) { this.pattern = pattern; this.kind = kind; }
    }

    private static final class Token {
        final String kind;
        final int start, end;
        Token(String kind, int start, int end) { this.kind = kind; this.start = start; this.end = end; }
    }

    /** Apply syntax highlighting for {@code language} ("js" or "nasm") to {@code pane}. */
    public static void highlight(JTextPane pane, String language, String text) {
        if (text == null) text = "";
        List<Token> tokens = tokenize(language, text);

        StyledDocument doc;
        if (pane.getDocument() instanceof StyledDocument){
            doc = (StyledDocument) pane.getDocument();
        } else {
            doc = new DefaultStyledDocument(StyleContext.getDefaultStyleContext());
            pane.setDocument(doc);
        }

        // Reset the entire existing range to the default (plain) style first,
        // so stale coloring from a previous highlight is cleared.
        int len = text.length();
        SimpleAttributeSet plain = new SimpleAttributeSet();
        StyleConstants.setFontFamily(plain, "Monospaced");
        StyleConstants.setFontSize(plain, 12);
        doc.setCharacterAttributes(0, len, plain, false);

        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet kw  = addStyle(sc, "keyword",  KEYWORD,   false, false);
        AttributeSet str = addStyle(sc, "string",   STRING,    false, false);
        AttributeSet com = addStyle(sc, "comment",  COMMENT,   false,  true);
        AttributeSet num = addStyle(sc, "number",   NUMBER,    false, false);
        AttributeSet reg = addStyle(sc, "register", REGISTER,  false, false);
        AttributeSet lab = addStyle(sc, "label",    LABEL,     true,  false);
        AttributeSet def = sc.getStyle(StyleContext.DEFAULT_STYLE);

        int pos = 0;
        for (Token t : tokens) {
            AttributeSet s = switchStyle(t.kind, kw, str, com, num, reg, lab, def);
            doc.setCharacterAttributes(t.start, t.end - t.start, s, false);
            pos = t.end;
        }
    }

    private static AttributeSet addStyle(StyleContext sc,
                                         String name, Color fg, boolean bold, boolean italic) {
        Style style = sc.addStyle(name, null);
        StyleConstants.setForeground(style, fg);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italic);
        return style;
    }

    private static AttributeSet switchStyle(String kind,
                                            AttributeSet kw, AttributeSet str, AttributeSet com,
                                            AttributeSet num, AttributeSet reg,
                                            AttributeSet lab, AttributeSet def) {
        switch (kind) {
            case "keyword": return kw;
            case "string":  return str;
            case "comment": return com;
            case "number":  return num;
            case "register": return reg;
            case "label":   return lab;
            default:        return def;
        }
    }

    private static List<Token> tokenize(String language, String text) {
        List<Rule> rules = rules(language);
        List<Token> tokens = new ArrayList<>();
        int i = 0, n = text.length();
        while (i < n) {
            boolean matched = false;
            for (Rule r : rules) {
                Matcher m = r.pattern.matcher(text);
                m.region(i, n);
                if (m.lookingAt()) {
                    if (r.kind != null) tokens.add(new Token(r.kind, i, m.end()));
                    i = m.end();
                    matched = true;
                    break;
                }
            }
            if (!matched) i++; // single character, default style
        }
        return tokens;
    }

    private static List<Rule> rules(String language) {
        List<Rule> out = new ArrayList<>();
        if ("js".equalsIgnoreCase(language)) {
            return jsRules(out);
        }
        if ("nasm".equalsIgnoreCase(language)) {
            return nasmRules(out);
        }
        return out;
    }

    // ---------- JavaScript ----------
    private static List<Rule> jsRules(List<Rule> out) {
        out.add(new Rule(Pattern.compile("[ \t\r\n]+"), null));
        out.add(new Rule(Pattern.compile("/\\*[\\s\\S]*?\\*/"), "comment"));
        out.add(new Rule(Pattern.compile("//[^\n]*"), "comment"));
        out.add(new Rule(Pattern.compile("\"([^\"\\\\]|\\\\.)*\""), "string"));
        out.add(new Rule(Pattern.compile("'([^'\\\\]|\\\\.)*'"), "string"));
        out.add(new Rule(Pattern.compile("\\d+(\\.\\d+)?"), "number"));
        out.add(new Rule(Pattern.compile(
                "\\b(var|let|const|if|else|while|for|function|return|true|false|null|undefined|" +
                "break|continue|throw|try|catch|finally|typeof|len|print|console)\\b"), "keyword"));
        out.add(new Rule(Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*"), null));
        out.add(new Rule(Pattern.compile("[\\s\\S]"), null));
        return out;
    }

    // ---------- NASM ----------
    private static List<Rule> nasmRules(List<Rule> out) {
        out.add(new Rule(Pattern.compile("[ \t\r\n]+"), null));
        out.add(new Rule(Pattern.compile(";[^\n]*"), "comment"));
        out.add(new Rule(Pattern.compile("\"[^\"]*\""), "string"));
        out.add(new Rule(Pattern.compile("0[xX][0-9a-fA-F]+"), "number"));
        out.add(new Rule(Pattern.compile("\\d+"), "number"));
        out.add(new Rule(Pattern.compile("\\b[A-Za-z_.][A-Za-z0-9_.]*\\s*:"), "label"));
        out.add(new Rule(Pattern.compile(
                "\\b(eax|ebx|ecx|edx|esi|edi|ebp|esp|" +
                "rax|rbx|rcx|rdx|rsi|rdi|rbp|rsp|" +
                "r8|r9|r10|r11|r12|r13|r14|r15|" +
                "r8d|r9d|r10d|r11d|r12d|r13d|r14d|r15d|" +
                "al|ah|bl|bh|cl|ch|dl|dh|" +
                "ax|bx|cx|dx|si|di|bp|sp|" +
                "sil|dil|bpl|spl|" +
                "cs|ds|es|ss|fs|gs|" +
                "ip|eip|rip|flags|eflags|rflags|" +
                "cr0|cr1|cr2|cr3|cr4|" +
                "dr0|dr1|dr2|dr3|dr6|dr7|" +
                "st|mm0|mm1|mm2|mm3|mm4|mm5|mm6|mm7|" +
                "xmm0|xmm1|xmm2|xmm3|xmm4|xmm5|xmm6|xmm7|" +
                "ymm0|ymm1|ymm2|ymm3|ymm4|ymm5|ymm6|ymm7)\\b"), "register"));
        out.add(new Rule(Pattern.compile(
                "\\b(mov|movsx|movzx|lea|add|sub|cmp|inc|dec|neg|mul|imul|div|idiv|" +
                "and|or|xor|not|shl|sal|shr|sar|rol|ror|rcl|rcr|" +
                "push|pop|pusha|popa|pushf|popf|pushad|popad|" +
                "call|ret|retn|iret|jmp|je|je|jne|jz|jnz|jl|jle|jg|jge|jb|jbe|ja|jae|" +
                "js|jns|jc|jnc|jo|jno|jp|jnp|loop|loope|loopne|" +
                "int|into|iret|nop|hlt|cli|sti|cld|std|clc|stc|" +
                "test|xchg|sete|setne|setl|setle|setg|setge|setb|setbe|seta|setae|" +
                "cmove|cmovne|cmovl|cmovle|cmovg|cmovge|cmovb|cmovbe|cmova|cmovae|" +
                "enter|leave|pusha|popa|" +
                "movsb|movsw|movsd|movsq|cmpsb|cmpsw|cmpsd|cmpsq|" +
                "scasb|scasw|scasd|scasq|" +
                "lodsb|lodsw|lodsd|lodsq|" +
                "stosb|stosw|stosd|stosq|" +
                "in|insb|insw|insd|insq|out|outsb|outsw|outsd|outsq|" +
                "rep|repe|repz|repne|repnz|" +
                "section|segment|global|extern|db|dw|dd|dq|dt|resb|resw|resd|resq|" +
                "times|equ|%define|%include|%ifdef|%ifndef|%endif|%if|%else|%endif)\\b"), "keyword"));
        out.add(new Rule(Pattern.compile("[A-Za-z_.][A-Za-z0-9_.]*"), null));
        out.add(new Rule(Pattern.compile("[\\s\\S]"), null));
        return out;
    }
}

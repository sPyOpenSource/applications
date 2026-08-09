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
        assertEquals("punc", t.get(6).kind);   // ;
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
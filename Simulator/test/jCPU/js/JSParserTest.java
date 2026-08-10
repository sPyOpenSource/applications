package jCPU.js;

import jCPU.js.ast.*;
import common.InterpreterException;
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
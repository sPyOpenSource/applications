package jCPU.js.ast;

public abstract class JSNode {
    public final int line;
    protected JSNode(int line) { this.line = line; }
}
package jCPU.js.ast;

public final class JSTryStmt extends JSStmt {
    public final JSBlock tryBlock;
    public final String catchVar;
    public final JSBlock catchBlock;
    public final JSBlock finallyBlock;
    public JSTryStmt(int line, JSBlock tryBlock, String catchVar, JSBlock catchBlock, JSBlock finallyBlock) {
        super(line);
        this.tryBlock = tryBlock;
        this.catchVar = catchVar;
        this.catchBlock = catchBlock;
        this.finallyBlock = finallyBlock;
    }
}
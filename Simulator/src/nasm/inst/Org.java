package nasm.inst;

import nasm.NasmVisitor;
import nasm.expr.Label;

public class Org extends PseudoInst {
    
    public Org(int nb, String comment){
        label = new Label("org");
	this.nb = nb;
	this.sizeInBytes = 1;
	this.comment = comment;
    }

    public <T> T accept(NasmVisitor <T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString(){
	return super.formatInst(this.label, "org", this.nb, this.comment);
    }

}

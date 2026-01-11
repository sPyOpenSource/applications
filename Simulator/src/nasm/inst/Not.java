package nasm.inst;

import nasm.Operand;
import nasm.NasmVisitor;

public class Not extends NasmInst {
    
    public Not(Operand label, Operand destination, String comment){
	destUse = true;
	destDef = true;
	this.label = label;
	this.destination = destination;
	this.comment = comment;
    }

    @Override
    public <T> T accept(NasmVisitor <T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString(){
	return super.formatInst(this.label, "not", this.destination, null, this.comment);
    }
    
    /**
     * Not (1/3 clks)
     * @param opr
     */
    public void notl(Operand opr) {
	realloc();
	insertByte(0xf7);
	insertModRM(2, opr);
    }
    
}

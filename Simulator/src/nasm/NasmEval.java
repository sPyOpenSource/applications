package nasm;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import jCPU.x86.X86Core;
import nasm.expr.*;
import nasm.inst.*;
import util.Memory;

public class NasmEval implements NasmVisitor<Integer>{
    private final Nasm code;
    private int dataSize = 0;

    private final int regNb;
    private final HashMap<String, Integer> labelToAddress;
    private final X86Core core;
    private boolean stop;
    private final ArrayList<Integer> output;

    private final int verboseLevel;

    public NasmEval(Nasm code, X86Core core, int stackSize, int verboseLevel){
        this.code = code;
        this.core = core;
	this.verboseLevel = verboseLevel;
	regNb = this.code.getTempCounter();
        stop = false;

        output = new ArrayList<>();
	labelToAddress = new HashMap<>();

	associateLabelToAddress();
       	core.setReg(X86Core.REG_ESP, core.getReg(X86Core.REG_ESP) - stackSize * 4);
    }

    public boolean isStopped() {
        return stop;
    }

    public void execute(){
        NasmInst inst = this.code.sectionText.get(core.getPC());
        if(verboseLevel > 0){
            System.out.println("--------------------------------------");
            PrintGlobalVariables();
            System.out.println("eip = " + core.getPC() + "\tesp = " + core.getReg(X86Core.REG_ESP) + "\t ebp = " + core.getReg(X86Core.REG_EBP));
            System.out.println("eax = " + core.getReg(X86Core.REG_EAX) + "\tebx = " + core.getReg(X86Core.REG_EBX) + "\tecx = " + core.getReg(X86Core.REG_ECX) + "\tedx = " + core.getReg(X86Core.REG_EDX));
            System.out.println("CF = " + core.getFlag(X86Core.FLAG_CF) + "\tPF = " + core.getFlag(X86Core.FLAG_PF) + "\tZF = " + core.getFlag(X86Core.FLAG_ZF) + "\tSF = " + core.getFlag(X86Core.FLAG_SF) + "\tOF = " + core.getFlag(X86Core.FLAG_OF));
            printRegisters();
            System.out.print("PILE : \t");
            printStack();
            System.out.println(inst);
        }
        core.setPC(inst.accept(this));
    }

    private void printStack() {
        int ss = core.getReg(X86Core.REG_ESP);
        for (int addr = ss + 12; addr > core.getReg(X86Core.REG_ESP); addr -= 4) {
            System.out.print(core.readMem32(addr) + " ");
        }
        System.out.println();
    }

    public void PrintGlobalVariables(){
        for (HashMap.Entry<String, Integer> e : labelToAddress.entrySet()){
            System.out.println(e.getKey() + " = " + core.readMem32(e.getValue()) + " adr: " + e.getValue());
        }
    }

    public void printRegisters(){
	for(int i = 0; i < regNb; i++){
	    System.out.print("r" + i + ":" + core.getReg(X86Core.REG_VIRTUAL_BASE + i) + "\t");
	}
    }

    public void displayOutput(){
        for(var val : output)
            System.out.println(val);
    }

    public void displayOutput(String outputFile) throws FileNotFoundException {
        var out = new PrintStream(outputFile);
        for(var val : output)
            out.println(val);
    }

    private void associateLabelToAddress(){
        var instructions = code.sectionText;
        for(int lineNb = 0; lineNb <instructions.size(); lineNb++){
            if(instructions.get(lineNb).label != null) {
                var label = (Label)instructions.get(lineNb).label;
                labelToAddress.put(label.val, lineNb);
            }
        }
	for(int i = 0; i < code.sectionBss.size(); i++){
	    PseudoInst pseudoInst = this.code.sectionBss.get(i);
	    labelToAddress.put(pseudoInst.label.val, dataSize);
	    dataSize += pseudoInst.nb * pseudoInst.sizeInBytes;
	}
    }

    /*------------------------------------------*/
    private void copy(Operand dest, int value){
	if(dest instanceof NasmAddress nasmAddress)
	    copy(nasmAddress, value);
	else if(dest instanceof NasmRegister nasmRegister)
	    copy(nasmRegister, value);
    }

    private void copy(NasmAddress dest, int value){
	int address = dest.val.accept(this);
	core.writeMem32(address, value);
    }

    private void copy(NasmRegister dest, int value){
	writeToRegister(dest, value);
    }

    /*------------------------------------------*/

    private int readFromRegister(NasmRegister reg){
	if(reg.color == Nasm.REG_EAX)
	    return core.getReg(X86Core.REG_EAX);
	if (reg.color == Nasm.REG_EBX)
	    return core.getReg(X86Core.REG_EBX);
	if (reg.color == Nasm.REG_ECX)
	    return core.getReg(X86Core.REG_ECX);
	if (reg.color == Nasm.REG_EDX)
	    return core.getReg(X86Core.REG_EDX);
	if (reg.color == Nasm.REG_ESP)
	    return core.getReg(X86Core.REG_ESP);
	if (reg.color == Nasm.REG_EBP)
	    return core.getReg(X86Core.REG_EBP);
	else
	    return core.getReg(X86Core.REG_VIRTUAL_BASE + reg.val);
    }

    private void writeToRegister(NasmRegister reg, int value){
	if(reg.color == Nasm.REG_EAX)
	    core.setReg(X86Core.REG_EAX, value);
	else if (reg.color == Nasm.REG_EBX)
	    core.setReg(X86Core.REG_EBX, value);
	else if (reg.color == Nasm.REG_ECX)
	    core.setReg(X86Core.REG_ECX, value);
	else if (reg.color == Nasm.REG_EDX)
	    core.setReg(X86Core.REG_EDX, value);
	else if (reg.color == Nasm.REG_ESP)
	    core.setReg(X86Core.REG_ESP, value);
	else if (reg.color == Nasm.REG_EBP)
	    core.setReg(X86Core.REG_EBP, value);
	else
	    core.setReg(X86Core.REG_VIRTUAL_BASE + reg.val, value);
    }

    /* visit address -> return the value stored at this address */
    @Override
    public Integer visit(NasmAddress adr) {
	return core.readMem32(adr.val.accept(this));
    }

    /* visit register -> return the value stored in the register */
    @Override
    public Integer visit(NasmRegister operand) {
	return readFromRegister(operand);
    }

    /* visit constant -> return value of the constant */
    @Override
    public Integer visit(NasmConstant operand) {
        return operand.val;
    }

    /* visit label -> return address corresponding to the label */
    @Override
    public Integer visit(Label operand) {
        if(labelToAddress.containsKey(operand.val))
	    return labelToAddress.get(operand.val);
	else
	    throw new RuntimeException("label " + operand.val + "does not correspond to address");
    }

    /* visiting an instruction returns new value of eip */
    /* arithmetic operations */

    @Override
    public Integer visit(Add inst) {
	copy(inst.destination, inst.source.accept(this) + inst.destination.accept(this));
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(Sub inst) {
        copy(inst.destination, inst.destination.accept(this) - inst.source.accept(this));
	return core.getPC() + 1;
    }

    @Override
    public Integer visit(Mul inst) {
        copy(inst.destination, inst.source.accept(this) * inst.destination.accept(this));
	return core.getPC() + 1;
    }

    @Override
    public Integer visit(Div inst) {
        var divisor  = inst.source.accept(this);
        var temp = core.getReg(X86Core.REG_EAX);
        core.setReg(X86Core.REG_EAX, temp / divisor);
        core.setReg(X86Core.REG_EDX, temp % divisor);
	return core.getPC() + 1;
    }

    /* logical operations */
    @Override
    public Integer visit(Or inst) {
        copy(inst.destination, inst.source.accept(this) | inst.destination.accept(this));
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(Not inst) {
        copy(inst.destination, ~ inst.destination.accept(this));
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(Xor inst) {
        copy(inst.destination, inst.source.accept(this) ^ inst.destination.accept(this));
	return core.getPC() + 1;
    }

    @Override
    public Integer visit(And inst) {
        copy(inst.destination, inst.source.accept(this) & inst.destination.accept(this));
	return core.getPC() + 1;
    }

    /* function call */
    @Override
    public Integer visit(Call inst) {
        if(inst.address instanceof Label && ((Label)inst.address).val.equals("iprintLF")){
            output.add(core.getReg(X86Core.REG_EAX));
	    return core.getPC() + 1;
	}
	core.push32(core.getPC());
	return inst.address.accept(this);
    }

    /* comparison */
    @Override
    public Integer visit(Cmp inst) {
        int valSrc = inst.source.accept(this);
        int valDest = inst.destination.accept(this);
        core.setFlag(X86Core.FLAG_ZF, valDest == valSrc);
	core.setFlag(X86Core.FLAG_SF, valDest < valSrc);
        return core.getPC() + 1;
    }

    /* jumps */
    @Override
    public Integer visit(Je inst) {
        return core.getFlag(X86Core.FLAG_ZF) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jle inst) {
        return (core.getFlag(X86Core.FLAG_ZF) || core.getFlag(X86Core.FLAG_SF)) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jne inst) {
        return (!core.getFlag(X86Core.FLAG_ZF)) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jge inst) {
        return (core.getFlag(X86Core.FLAG_ZF) || !core.getFlag(X86Core.FLAG_SF)) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jl inst) {
	return (!core.getFlag(X86Core.FLAG_ZF) && core.getFlag(X86Core.FLAG_SF)) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jg inst) {
        return (!core.getFlag(X86Core.FLAG_ZF) || core.getFlag(X86Core.FLAG_SF)) ? inst.address.accept(this) : core.getPC() + 1;
    }

    @Override
    public Integer visit(Jmp inst) {
        return inst.address.accept(this);
    }

    @Override
    public Integer visit(Pop inst) {
        copy(inst.destination, core.pop32());
	return core.getPC() + 1;
    }

    @Override
    public Integer visit(Push inst) {
        core.push32(inst.source.accept(this));
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(Ret inst) {
        return core.pop32() + 1;
    }

    @Override
    public Integer visit(Mov inst) {
        copy(inst.destination, inst.source.accept(this));
	return core.getPC() + 1;
    }

    @Override
    public Integer visit(Int inst) {
        if(core.getReg(X86Core.REG_EAX) == 1)
            stop = true;
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(NasmInst inst) {
        return 0;
    }

    @Override
    public Integer visit(Empty inst) {
        return core.getPC() + 1;
    }

    @Override
    public Integer visit(Resb pseudoInst){return 0;}
    @Override
    public Integer visit(Resw pseudoInst){return 0;}
    @Override
    public Integer visit(Resd pseudoInst){return 0;}
    @Override
    public Integer visit(Resq pseudoInst){return 0;}
    @Override
    public Integer visit(Rest pseudoInst){return 0;}

    /* visit expression -> returns an address */

    @Override
    public Integer visit(NasmExp exp) {
	if(exp instanceof Label label)
	    return label.accept(this);

	if(exp instanceof NasmRegister nasmRegister)
	    return nasmRegister.accept(this);

	if(exp instanceof Label label)
	    return label.accept(this);

	if(exp instanceof NasmConstant nasmConstant)
	    return nasmConstant.accept(this);

	if(exp instanceof ExpPlus expPlus)
	    return expPlus.accept(this);

	if(exp instanceof ExpMinus expMinus)
	    return expMinus.accept(this);

	    return ((ExpTimes)exp).accept(this);
    }
    @Override
    public Integer visit(ExpPlus exp) {return exp.op1.accept(this) + exp.op2.accept(this);}
    @Override
    public Integer visit(ExpMinus exp){return exp.op1.accept(this) - exp.op2.accept(this);}
    @Override
    public Integer visit(ExpTimes exp){return exp.op1.accept(this) * exp.op2.accept(this);}

    @Override
    public Integer visit(Equ pseudoInst) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Integer visit(Org pseudoInst) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

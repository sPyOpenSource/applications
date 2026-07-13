package jx.verifier.bytecode;

import jx.classfile.constantpool.*;
import jx.verifier.*;

public class ByteCode implements jx.zero.ByteCode {
    
    //for a double linked list of bytecodes
    public ByteCode prev = null;
    public ByteCode next = null;

    public boolean isTarget = false;
    public JVMState beforeState = null; //state of the jvm just before executing this bc
    public int mvCheckCount = 0; //counts how often this is in checkQueue of a methodverifier
    public int svCheckCount = 0; //how often in checkQueue of SubroutineVerifier

    protected int opCode; //opCode of this bc
    protected byte[] byteArgs; //arguments from the bytecode for this operation
    protected int address; //address in the bytecode (0 = start)

    protected ByteCode[] targets; //all oprations that might possibly executed after this one. ExceptionHandlers are held separately
    protected ByteCode[] sources; //all operations that might have been executed just before this one. Again without exceptions
    public ExceptionHandler[] eHandlers; //all ExceptionHandlers that protect this bytecode
    public ExceptionHandler startsEH = null; //if this operation is the first op. of an exception handler, startsEH is a reference to this exception handler; normally null

    @Override
    public ByteCode[] getTargets() {return targets;}
    @Override
    public ByteCode[] getSources() {return sources;}

    public void addSource(ByteCode newEntry) {
	//FEHLER sehr aufwndig, vielleicht gehts anders einfacher.
	ByteCode[] ns = new ByteCode[sources.length+1];
	for (int i = 0; i < sources.length; i++) {
	    if (newEntry.getAddress() == sources[i].getAddress())
		return; //entry already registered!
	    ns[i] = sources[i];
	}
	ns[ns.length-1] = newEntry;
	sources = ns;
    }

     public String getBCName() {return getBCName(opCode);}
    public static String getBCName(int opCode) { 
	return ((opCode >= 0) && (opCode < OPNAMES.length))? OPNAMES[opCode] : null;}
    public int getOpCode() { return opCode;}
    public String getByteArgsString() {
	String ret = "";
	if (byteArgs == null) return ret;
	for (int i = 0; i < byteArgs.length; i++) {
	    ret += Integer.toHexString(byteArgs[i] &0xff) + " ";
	}
	return ret;
    }
    public byte[] getByteArgs() { return byteArgs;}
    public int getAddress() {return address;}
    public void setAddress(int newAddress) {address = newAddress;}

    @Override
    public String toString() {
	return Integer.toHexString(address) + ": " +
	    getBCName() + "("+ Integer.toHexString(opCode) +") "+
	    getByteArgsString();
    }
	    
    //returns the size of this Bytecode in Bytes
    public int getSize() {
	return ((byteArgs != null)? byteArgs.length : 0) + 1; //arguments + 1 byte for opCode
    }
    
    protected ByteCode(int opCode, ByteIterator code){
	this.opCode = opCode;
	sources = new ByteCode[0];

	address = code.getIndex();
	if (OPNUMARGS[opCode] > 0) {
	    byteArgs = new byte[OPNUMARGS[opCode]];
	    for (int i = 0; i < byteArgs.length; i++) {
		byteArgs[i] = code.getNext();
	    }
	}
	
    }

    /** Create new Bytecode from codeBytes.
     * the address of the newly created bytecode is -1.
     */
    public ByteCode(byte[] codeBytes) {
	this.opCode = ((int) codeBytes[0])&0xff;
	sources = new ByteCode[0];
	address = -1;
	if (OPNUMARGS[opCode] > 0) {
	    byteArgs = new byte[OPNUMARGS[opCode]];
	    if (codeBytes.length < byteArgs.length+1) 
		throw new Error("Internal Error!");
	    for (int i = 0; i < byteArgs.length; i++) {
		byteArgs[i] = codeBytes[i+1];
	    }
	}
    }

    /**Adds the bytecode for this instruction to the bytecode array.
     * @param index the position where the bytecode should be written.
     * @param array the array to which the bc. should be written.
     * @return the index of the first byte after the bytes just added.
     */
    public int toByteArray(int index, byte[] array) {
	int ret = index;
	array[ret++] = (byte)opCode;
	if (byteArgs != null) {
	    for (int i = 0; i < byteArgs.length; i++)
		array[ret++] = byteArgs[i];
	}
	return ret;
    }

    public static ByteCode newByteCode(ByteIterator code, ConstantPool cPool, ByteCode prev) {
	return newByteCode(code.getNext(), code, cPool, prev);
    }
    public static ByteCode newByteCode(byte opCode, ByteIterator code, ConstantPool cPool, ByteCode prev) {
	int intOpCode = ((int)opCode)&0xff;
	return newByteCode(intOpCode, code, cPool, prev);
    }
    
    public static ByteCode newByteCode(int opCode, ByteIterator code, ConstantPool cPool, ByteCode prev) {
	if ((opCode < 0) || opCode >= OPNAMES.length) {
	    //FEHLER kein internal error, sondern ein verifyerror!
	    throw new Error("Internal Error: newByteCode called with invalid opCode (" + opCode + ")!");
	}
	if (code == null) {
	    throw new Error("Internal Error: newByteCode called code == null");
	}
	
	ByteCode retVal = null;
	switch (opCode) {
	case IFEQ:
	case IFNE:
	case IFLT:
	case IFGE:
	case IFGT:
	case IFLE:
	case IF_ICMPEQ:
	case IF_ICMPNE:
	case IF_ICMPLT:
	case IF_ICMPGE:
	case IF_ICMPGT:
	case IF_ICMPLE:
	case IF_ACMPEQ:
	case IF_ACMPNE:
	case IFNULL:
	case IFNONNULL:
	case GOTO:
	case JSR:
	case JSR_W:
	case RET:
	    retVal = new BCBranch(opCode, code);
	    break;
	case TABLESWITCH:
	case LOOKUPSWITCH:
	    retVal = new BCMultiBranch(opCode, code);
	    break;
	case WIDE:
	    retVal = new BCWideOp(opCode, code);
	    break;
	case ANEWARRAY:
	case CHECKCAST:
	case GETFIELD:
	case GETSTATIC:
	case INSTANCEOF:
	case INVOKEINTERFACE:
	case INVOKESPECIAL:
	case INVOKESTATIC:
	case INVOKEVIRTUAL:
	case LDC:
	case LDC_W:
	case LDC2_W:
	case MULTIANEWARRAY:
	case NEW:
	case PUTFIELD:
	case PUTSTATIC:
	    if (cPool == null) {
		throw new Error("Internal Error: newByteCode called with cPool == null!");
	    }
	    retVal = new BCCPArgOp(opCode, code, cPool);
	    break;
	default:
	    retVal = new ByteCode(opCode, code);
	}
	if (prev != null) {
	    retVal.prev = prev;
	    retVal.next = prev.next;
	    prev.next = retVal;
	    if (retVal.next != null) {
		retVal.next.prev = retVal;
	    }
	}
	return retVal;
    }

    public void linkTargets(BCLinkList bcl, ByteCode next) throws VerifyException { 
	switch(opCode) {
	case RETURN:
	case IRETURN:
	case LRETURN:
	case FRETURN:
	case DRETURN:
	case ARETURN:
	case ATHROW:
	    targets = new ByteCode[0];
	    break;
	default:
	    if(next != null) {
		targets = new ByteCode[1];
		targets[0] = next;
	    } else
		targets = new ByteCode[0];
	    
	}
        for (ByteCode target : targets) {
            target.addSource(this);
        }
    }
    public void recomputeTargetAddresses() {
	//nothing to do for ordinary Bytecodes.
    }

    @Override
    public jx.zero.ByteCode getPrev() {
        return prev;    
    }

    @Override
    public jx.zero.ByteCode getNext() {
        return next;    
    }

    @Override
    public boolean isTarget() {
        return isTarget;    
    }

    @Override
    public int mvCheckCount() {
        return mvCheckCount;    
    }

    @Override
    public int svCheckCount() {
        return svCheckCount;    
    }

    @Override
    public void mvCheckCount(int i) {
        mvCheckCount = i;
    }

    @Override
    public void svCheckCount(int i) {
        svCheckCount = i;
    }

}

package jx.verifier.checkInstruction;

import java.util.Vector;
import jx.verifier.VerifyException;
import jx.verifier.bytecode.BCLinkList;
import jx.zero.ByteCode;
import jx.zero.classfile.ConstantPool;
import jx.zero.classfile.MethodSource;
import jx.zero.classfile.VerifyResult;

public class checkInstruction {

    //check if instructions in opCodes is used in method
    static public void  verifyMethod(MethodSource method, 
				     ConstantPool cPool,
				     int[] opCodes) throws VerifyException {

	if (opCodes.length == 0)
	    return;

	BCLinkList code = new BCLinkList(method, cPool);

	Vector result = new Vector(opCodes.length);

	for (int i = 0; i < opCodes.length; i++) {
	    for (ByteCode actBc = code.getFirst(); 
		 actBc != null; 
		 actBc = actBc.getNext()) {
		if (actBc.getOpCode() == opCodes[i]){
		    result.addElement(opCodes[i]);
		}
	    }
	}
	method.setVerifyResult(new cInstrResult(opCodes ,result));

    }

    //check if method uses new instruction
    static public boolean usesNewInstruction(MethodSource method,
					     ConstantPool cPool) throws VerifyException{
	cInstrResult res = (cInstrResult)method.getVerifyResult(VerifyResult.CINSTR_RESULT);
	if (res == null || res.instructionUsed(ByteCode.NEW) == 0) {
	    int tmp[] = {ByteCode.NEW};
	    verifyMethod(method, cPool, tmp);
	    res = (cInstrResult)method.getVerifyResult(VerifyResult.CINSTR_RESULT);
	}
        return res.instructionUsed(ByteCode.NEW) == 1;
    }
    
}

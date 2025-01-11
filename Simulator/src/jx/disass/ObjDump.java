package jx.disass;

import java.io.FileInputStream;
import java.util.ArrayList;
import jx.compiler.persistent.CodeFile;
import jx.compiler.persistent.CompiledClass;
import jx.compiler.persistent.CompiledMethod;
import jx.compiler.persistent.ExtendedDataInputStream;

public class ObjDump {
    public static void main(String [] args) throws Exception {
        args = new String[]{"/Users/xuyi/Source/OS/armOS/lib/jcore/Compiler/app/isodir/code/zero.jll"};
    	if (args.length != 1) {
	    System.out.println("Usage: ");
	    System.out.println("    ObjDump <codefilename> ");
	    return;
	}
	ExtendedDataInputStream stream = new ExtendedDataInputStream(new FileInputStream(args[0]));
	CodeFile file = new CodeFile(null, null);
	ArrayList<CompiledClass> allClasses = file.read(stream);
	for(CompiledClass compiledClass : allClasses) {
	    System.out.println("Class: " + compiledClass.getName());
	    CompiledMethod[] allMethods = compiledClass.getMethods();
            for (CompiledMethod compiledMethod : allMethods) {
                byte[] code = compiledMethod.getCode();
                if (code == null) {
                    System.out.print("  Size:  - ");
                } else {
                    System.out.println("  Size: " + compiledMethod.getCode().length);
                    Disassembler dis = new Disassembler(code);
                    for(int i = 0; dis.isNext(); i++){
                        dis.instruction = dis.toHexInt(i) + " ";
                        System.out.println(dis.disasmInstr());
                    }
                }
                System.out.println(" Method: " + compiledMethod.getName());
            }
        }
    }
}

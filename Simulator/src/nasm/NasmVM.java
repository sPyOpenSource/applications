package nasm;

import j51.intel.MCS51;
import java.io.IOException;
import jCPU.x86.X86Core;
import nasm.inst.NasmInst;

public class NasmVM extends MCS51 {
    private final int verboseLevel = 0;
    private final int stackSize = 10000;
    private final X86Core core;
    private Nasm code;
    NasmEval eval;

    public NasmVM(){
        this(64 * 1024 * 1024);
    }

    public NasmVM(int memSize){
        core = new X86Core(memSize);
    }

    public X86Core getCore() {
        return core;
    }

    public NasmEval getEval() {
        return eval;
    }

    public Nasm getCode() {
        return code;
    }

    public void init(String nasmFileName) throws IOException {
	LoadNasm loadNasm = new LoadNasm(nasmFileName);
	this.code = loadNasm.getNasm();

	if(verboseLevel > 0)
       		code.afficheNasm(null);

        eval = new NasmEval(code, core, stackSize, verboseLevel);
    }

    @Override
    public int step(){
        eval.execute();
        return 1;
    }

    @Override
    public String getDecodeAt(int pc)
    {
        try{
            NasmInst inst = this.code.sectionText.get(pc);
            return "     " + inst.toString();
        } catch (Exception e) { return "     "; }
    }

    public static void main(String[] args){
	int verboseLevel = 0;
	int stackSize = 10000;
	String nasmFileName = "/Users/xuyi/Source/Java/picos/bts_dsk.yasm";
        nasmFileName = "test2024/nasm-ref/incr1.nasm";
        try {
	    for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-v":
                        verboseLevel = Integer.parseInt(args[++i]);
                        break;
                    case "-s":
                        stackSize = Integer.parseInt(args[++i]);
                        break;
                    case "-nasm":
                        nasmFileName = args[++i];
                        break;
                    default:
                        break;
                }
	    }
	    if(nasmFileName == null){
		System.out.println("java NasmVM -nasm nasmFile -s stackSize -v verboseLevel");
		System.exit(1);
	    }
	    var vm = new NasmVM();
	} catch(Exception e) {
            e.printStackTrace();
        }
    }
}

package nasm;

import j51.intel.MCS51;
import java.io.IOException;
import nasm.inst.NasmInst;

public class NasmVM extends MCS51{
    private final int verboseLevel = 0;
    private final int stackSize = 10000;
    private Nasm code;
    NasmEval eval;

    public NasmVM(){
        try {
            init("/Users/xuyi/Source/Java/nasm/test2024/nasm-ref/incr1.nasm");
            //init("/Users/xuyi/Source/Java/picos/bts_dsk.yasm");
        } catch (IOException ex) {
            System.getLogger(NasmVM.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    public void init(String nasmFileName) throws IOException {
	LoadNasm loadNasm = new LoadNasm(nasmFileName);
	this.code = loadNasm.getNasm();

	if(verboseLevel > 0)
       		code.afficheNasm(null);
	
        eval = new NasmEval(code, stackSize, verboseLevel);
	//eval.displayOutput();
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
	    //vm.run();
	} catch(Exception e) {
            e.printStackTrace();
        }
    }
}

/*
        else{
            var pathTab = preNasmFilePath.split("/");
            var fileNamePreNasm = pathTab[pathTab.length - 1];
            var fileName =  outputPath + fileNamePreNasm.substring(0, fileNamePreNasm.length()-3)+ ".out";
            vm.displayOutput(fileName);
        }
    }
*/

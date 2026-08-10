package nasm;

import jCPU.iCPU;
import java.io.IOException;
import jCPU.x86.X86Core;
import jCPU.x86.X86VgaPeripheral;
import jCPU.x86.X86UartPeripheral;
import nasm.inst.NasmInst;

public class NasmVM implements iCPU {
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
        X86VgaPeripheral vga = new X86VgaPeripheral();
        for (int port = 0x3C0; port <= 0x3DA; port++) {
            core.registerPort(port, vga);
        }
        X86UartPeripheral uart = new X86UartPeripheral();
        for (int port = 0x3F8; port <= 0x3FF; port++) {
            core.registerPort(port, uart);
        }
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
	} catch(NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void go(int i) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

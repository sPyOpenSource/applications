package jx.disass;


public class x86 extends j51.intel.MCS51 {
    static String[] sreg_mod01_rm32   = {"DS", "DS", "DS", "DS", "??", "SS", "DS", "DS"};
    static String[] sreg_mod10_rm32   = {"DS", "DS", "DS", "DS", "??", "SS", "DS", "DS"};
    static String[] sreg_mod00_base32 = {"DS", "DS", "DS", "DS", "SS", "DS", "DS", "DS"};
    static String[] sreg_mod01_base32 = {"DS", "DS", "DS", "DS", "SS", "SS", "DS", "DS"};
    static String[] sreg_mod10_base32 = {"DS", "DS", "DS", "DS", "SS", "SS", "DS", "DS"};
    static String[] sreg_mod00_rm16   = {"DS", "SS", "SS", "DS", "DS", "DS", "DS"};
    static String[] sreg_mod01_rm16   = {"DS", "DS", "SS", "SS", "DS", "DS", "DS", "DS"};
    static String[] sreg_mod10_rm16   = {"DS", "DS", "SS", "SS", "DS", "DS", "SS", "DS"};
    static String[] segment_name      = {"ES", "CS", "SS", "DS", "FS", "GS", "??", "??"};
    static String[] general_8bit_reg_name  = { "AL",  "CL",  "DL",  "BL",  "AH",  "CH",  "DH",  "BH"};
    static String[] general_16bit_reg_name = { "AX",  "CX",  "DX",  "BX",  "SP",  "BP",  "SI",  "DI"};
    static String[] general_32bit_reg_name = {"EAX", "ECX", "EDX", "EBX", "ESP", "EBP", "ESI", "EDI"};
    static String[] base_name16  = { "BX",  "BX",  "BP",  "BP",  "??",  "??",  "BP",  "BX"};
    static String[] index_name16 = { "SI",  "DI",  "SI",  "DI",  "SI",  "DI",  "??",  "??"};
    static String[] index_name32 = {"EAX", "ECX", "EDX", "EBX", "???", "EBP", "ESI", "EDI"};

    static final int BX_SEGMENT_REG       = 10;
    static final int BX_GENERAL_8BIT_REG  = 11;
    static final int BX_GENERAL_16BIT_REG = 12;
    static final int BX_GENERAL_32BIT_REG = 13;
    static final int BX_NO_REG_TYPE       = 14;
    
    public static final int EAX = 0;
    public static final int ECX = 1;
    public static final int EDX = 2;
    public static final int EBX = 3;
    public static final int ESP = 4;
    public static final int EBP = 5;
    public static final int ESI = 6;
    public static final int EDI = 7;
    
    public static void main(String [] args){
        Disassembler dis = new Disassembler("/Users/xuyi/Source/OS/armOS/lib/jcore/Compiler/app/isodir/code/realmode", 2000, 0x1000);
        for(int i = 0; i < 200; i++){
            dis.instruction = dis.toHexInt(i) + " ";
            System.out.println(dis.disasmInstr());
            //dis.instruction = "";
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AI.Models.vm;

import AI.Models.JVM;
import java.util.function.Function;

/**
 *
 * @author spy
 */
public class VmByteCode {
    private int opCode;
    public Function func;
    public int offset;
    public String name;
    
    private Function<JVM, Integer> op_aload_0 = cpu -> cpu.op_aload_0();
    private Function<JVM, Integer> op_bipush = cpu -> cpu.op_bipush();
    private Function<JVM, Integer> op_dup, op_get, op_iadd, op_iconst_0;
    private Function<JVM, Integer> op_iconst_1, op_iconst_2;
    private Function<JVM, Integer> op_iconst_3, op_iconst_4, op_iconst_5;
    private Function<JVM, Integer> op_dconst_1, op_idiv, op_imul, op_dadd, op_dmul, op_d2i;
    private Function<JVM, Integer> op_invokespecial, op_invokevirtual;
    private Function<JVM, Integer> op_invoke = cpu -> cpu.op_invoke();
    private Function<JVM, Integer> op_iload, op_iload_1, op_iload_2, op_iload_3;
    private Function<JVM, Integer> op_istore, op_istore_1, op_istore_2, op_istore_3;
    private Function<JVM, Integer> op_isub, op_ldc, op_ldc2_w;
    private Function<JVM, Integer> op_new, op_irem, op_sipush, op_return;

    public VmByteCode(String name, int opCode, int offset, Function func){
        this.opCode = opCode;
        this.name = name;
        this.offset = offset;
        this.func = func;
    }
    
    public VmByteCode(int opCode){
        this.opCode = opCode;
    }
    
    public char[] getBytecode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmCP getCP() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public VmMethod getMethod() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    VmByteCode byteCodes[] = {
        new VmByteCode("aload_0"        , 0x2A, 1, op_aload_0       ),
        new VmByteCode( "bipush"        , 0x10, 2, op_bipush        ),
        new VmByteCode( "dup"           , 0x59, 1, op_dup           ),
        new VmByteCode( "get"           , 0xB2, 3, op_get           ),
        new VmByteCode( "iadd"          , 0x60, 1, op_iadd          ),
        new VmByteCode( "iconst_0"      , 0x03, 1, op_iconst_0      ),
        new VmByteCode( "iconst_1"      , 0x04, 1, op_iconst_1      ),
        new VmByteCode("iconst_2"       , 0x05, 1, op_iconst_2      ),
        new VmByteCode( "iconst_3"      , 0x06, 1, op_iconst_3      ),
        new VmByteCode( "iconst_4"      , 0x07, 1, op_iconst_4      ),
        new VmByteCode( "iconst_5"      , 0x08, 1, op_iconst_5      ),
        new VmByteCode( "dconst_1"      , 0x0F, 1, op_dconst_1      ),
        new VmByteCode( "idiv"          , 0x6C, 1, op_idiv          ),
        new VmByteCode( "imul"          , 0x68, 1, op_imul          ),
        new VmByteCode( "dadd"          , 0x63, 1, op_dadd          ),
        new VmByteCode( "dmul"          , 0x6B, 1, op_dmul          ),
        new VmByteCode( "d2i"           , 0x8e, 1, op_d2i           ),
        new VmByteCode( "invokespecial" , 0xB7, 3, op_invokespecial ),
        new VmByteCode( "invokevirtual" , 0xB6, 3, op_invokevirtual ),
        new VmByteCode( "invoke"        , 0xB8, 3, op_invoke        ),
        new VmByteCode( "iload"         , 0x15, 2, op_iload         ),
        new VmByteCode( "iload_1"       , 0x1B, 1, op_iload_1       ),
        new VmByteCode( "iload_2"       , 0x1C, 1, op_iload_2       ),
        new VmByteCode( "iload_3"       , 0x1D, 1, op_iload_3       ),
        new VmByteCode( "istore"        , 0x36, 2, op_istore        ),
        new VmByteCode( "istore_1"      , 0x3C, 1, op_istore_1      ),
        new VmByteCode( "istore_2"      , 0x3D, 1, op_istore_2      ),
        new VmByteCode( "istore_3"      , 0x3E, 1, op_istore_3      ),
        new VmByteCode( "isub"          , 0x64, 1, op_isub          ),
        new VmByteCode( "ldc"           , 0x12, 2, op_ldc           ),
        new VmByteCode( "ldc2_w"        , 0x14, 3, op_ldc2_w        ),
        new VmByteCode( "new"           , 0xBB, 3, op_new           ),
        new VmByteCode( "irem"          , 0x70, 1, op_irem          ),
        new VmByteCode( "sipush"        , 0x11, 3, op_sipush        ),
        new VmByteCode( "return"        , 0xB1, 1, op_return        )
    };
    
    public VmByteCode findOpCode()
    {
        for (VmByteCode byteCode : byteCodes) {
            if (opCode == byteCode.opCode) {
                name = byteCode.name;
                return byteCode;
            }
        }
        return null;
    }
}

package jCPU.Wasm;

public class WasmConstants {
    // Section IDs
    public static final int SECTION_CUSTOM = 0;
    public static final int SECTION_TYPE = 1;
    public static final int SECTION_IMPORT = 2;
    public static final int SECTION_FUNCTION = 3;
    public static final int SECTION_TABLE = 4;
    public static final int SECTION_MEMORY = 5;
    public static final int SECTION_GLOBAL = 6;
    public static final int SECTION_EXPORT = 7;
    public static final int SECTION_START = 8;
    public static final int SECTION_ELEMENT = 9;
    public static final int SECTION_CODE = 10;
    public static final int SECTION_DATA = 11;

    // Value types
    public static final int TYPE_I32 = 0x7F;
    public static final int TYPE_I64 = 0x7E;
    public static final int TYPE_F32 = 0x7D;
    public static final int TYPE_F64 = 0x7C;
    public static final int TYPE_VOID = 0x40;

    // Opcodes - Control
    public static final int OP_UNREACHABLE = 0x00;
    public static final int OP_NOP = 0x01;
    public static final int OP_BLOCK = 0x02;
    public static final int OP_LOOP = 0x03;
    public static final int OP_IF = 0x04;
    public static final int OP_ELSE = 0x05;
    public static final int OP_END = 0x0B;
    public static final int OP_BR = 0x0C;
    public static final int OP_BR_IF = 0x0D;
    public static final int OP_BR_TABLE = 0x0E;
    public static final int OP_RETURN = 0x0F;
    public static final int OP_CALL = 0x10;
    public static final int OP_CALL_INDIRECT = 0x11;

    // Opcodes - Parametric
    public static final int OP_DROP = 0x1A;
    public static final int OP_SELECT = 0x1B;

    // Opcodes - Variable
    public static final int OP_LOCAL_GET = 0x20;
    public static final int OP_LOCAL_SET = 0x21;
    public static final int OP_LOCAL_TEE = 0x22;
    public static final int OP_GLOBAL_GET = 0x23;
    public static final int OP_GLOBAL_SET = 0x24;

    // Opcodes - Memory
    public static final int OP_I32_LOAD = 0x28;
    public static final int OP_I64_LOAD = 0x29;
    public static final int OP_F32_LOAD = 0x2A;
    public static final int OP_F64_LOAD = 0x2B;
    public static final int OP_I32_LOAD8_S = 0x2C;
    public static final int OP_I32_LOAD8_U = 0x2D;
    public static final int OP_I32_LOAD16_S = 0x2E;
    public static final int OP_I32_LOAD16_U = 0x2F;
    public static final int OP_I64_LOAD8_S = 0x30;
    public static final int OP_I64_LOAD8_U = 0x31;
    public static final int OP_I64_LOAD16_S = 0x32;
    public static final int OP_I64_LOAD16_U = 0x33;
    public static final int OP_I64_LOAD32_S = 0x34;
    public static final int OP_I64_LOAD32_U = 0x35;
    public static final int OP_I32_STORE = 0x36;
    public static final int OP_I64_STORE = 0x37;
    public static final int OP_F32_STORE = 0x38;
    public static final int OP_F64_STORE = 0x39;
    public static final int OP_I32_STORE8 = 0x3A;
    public static final int OP_I32_STORE16 = 0x3B;
    public static final int OP_I64_STORE8 = 0x3C;
    public static final int OP_I64_STORE16 = 0x3D;
    public static final int OP_I64_STORE32 = 0x3E;
    public static final int OP_MEMORY_SIZE = 0x3F;
    public static final int OP_MEMORY_GROW = 0x40;

    // Opcodes - Numeric constants
    public static final int OP_I32_CONST = 0x41;
    public static final int OP_I64_CONST = 0x42;
    public static final int OP_F32_CONST = 0x43;
    public static final int OP_F64_CONST = 0x44;

    // Opcodes - i32 comparison
    public static final int OP_I32_EQZ = 0x45;
    public static final int OP_I32_EQ = 0x46;
    public static final int OP_I32_NE = 0x47;
    public static final int OP_I32_LT_S = 0x48;
    public static final int OP_I32_LT_U = 0x49;
    public static final int OP_I32_GT_S = 0x4A;
    public static final int OP_I32_GT_U = 0x4B;
    public static final int OP_I32_LE_S = 0x4C;
    public static final int OP_I32_LE_U = 0x4D;
    public static final int OP_I32_GE_S = 0x4E;
    public static final int OP_I32_GE_U = 0x4F;

    // Opcodes - i64 comparison
    public static final int OP_I64_EQZ = 0x50;
    public static final int OP_I64_EQ = 0x51;
    public static final int OP_I64_NE = 0x52;
    public static final int OP_I64_LT_S = 0x53;
    public static final int OP_I64_LT_U = 0x54;
    public static final int OP_I64_GT_S = 0x55;
    public static final int OP_I64_GT_U = 0x56;
    public static final int OP_I64_LE_S = 0x57;
    public static final int OP_I64_LE_U = 0x58;
    public static final int OP_I64_GE_S = 0x59;
    public static final int OP_I64_GE_U = 0x5A;

    // Opcodes - f32 comparison
    public static final int OP_F32_EQ = 0x5B;
    public static final int OP_F32_NE = 0x5C;
    public static final int OP_F32_LT = 0x5D;
    public static final int OP_F32_GT = 0x5E;
    public static final int OP_F32_LE = 0x5F;
    public static final int OP_F32_GE = 0x60;

    // Opcodes - f64 comparison
    public static final int OP_F64_EQ = 0x61;
    public static final int OP_F64_NE = 0x62;
    public static final int OP_F64_LT = 0x63;
    public static final int OP_F64_GT = 0x64;
    public static final int OP_F64_LE = 0x65;
    public static final int OP_F64_GE = 0x66;

    // Opcodes - i32 arithmetic
    public static final int OP_I32_CLZ = 0x67;
    public static final int OP_I32_CTZ = 0x68;
    public static final int OP_I32_POPCNT = 0x69;
    public static final int OP_I32_ADD = 0x6A;
    public static final int OP_I32_SUB = 0x6B;
    public static final int OP_I32_MUL = 0x6C;
    public static final int OP_I32_DIV_S = 0x6D;
    public static final int OP_I32_DIV_U = 0x6E;
    public static final int OP_I32_REM_S = 0x6F;
    public static final int OP_I32_REM_U = 0x70;
    public static final int OP_I32_AND = 0x71;
    public static final int OP_I32_OR = 0x72;
    public static final int OP_I32_XOR = 0x73;
    public static final int OP_I32_SHL = 0x74;
    public static final int OP_I32_SHR_S = 0x75;
    public static final int OP_I32_SHR_U = 0x76;
    public static final int OP_I32_ROTR = 0x77;
    public static final int OP_I32_ROTL = 0x78;

    // Opcodes - i64 arithmetic
    public static final int OP_I64_CLZ = 0x79;
    public static final int OP_I64_CTZ = 0x7A;
    public static final int OP_I64_POPCNT = 0x7B;
    public static final int OP_I64_ADD = 0x7C;
    public static final int OP_I64_SUB = 0x7D;
    public static final int OP_I64_MUL = 0x7E;
    public static final int OP_I64_DIV_S = 0x7F;
    public static final int OP_I64_DIV_U = 0x80;
    public static final int OP_I64_REM_S = 0x81;
    public static final int OP_I64_REM_U = 0x82;
    public static final int OP_I64_AND = 0x83;
    public static final int OP_I64_OR = 0x84;
    public static final int OP_I64_XOR = 0x85;
    public static final int OP_I64_SHL = 0x86;
    public static final int OP_I64_SHR_S = 0x87;
    public static final int OP_I64_SHR_U = 0x88;
    public static final int OP_I64_ROTR = 0x89;
    public static final int OP_I64_ROTL = 0x8A;

    // Opcodes - f32 arithmetic
    public static final int OP_F32_ABS = 0x8B;
    public static final int OP_F32_NEG = 0x8C;
    public static final int OP_F32_CEIL = 0x8D;
    public static final int OP_F32_FLOOR = 0x8E;
    public static final int OP_F32_TRUNC = 0x8F;
    public static final int OP_F32_NEAREST = 0x90;
    public static final int OP_F32_SQRT = 0x91;
    public static final int OP_F32_ADD = 0x92;
    public static final int OP_F32_SUB = 0x93;
    public static final int OP_F32_MUL = 0x94;
    public static final int OP_F32_DIV = 0x95;
    public static final int OP_F32_MIN = 0x96;
    public static final int OP_F32_MAX = 0x97;
    public static final int OP_F32_COPYSIGN = 0x98;

    // Opcodes - f64 arithmetic
    public static final int OP_F64_ABS = 0x99;
    public static final int OP_F64_NEG = 0x9A;
    public static final int OP_F64_CEIL = 0x9B;
    public static final int OP_F64_FLOOR = 0x9C;
    public static final int OP_F64_TRUNC = 0x9D;
    public static final int OP_F64_NEAREST = 0x9E;
    public static final int OP_F64_SQRT = 0x9F;
    public static final int OP_F64_ADD = 0xA0;
    public static final int OP_F64_SUB = 0xA1;
    public static final int OP_F64_MUL = 0xA2;
    public static final int OP_F64_DIV = 0xA3;
    public static final int OP_F64_MIN = 0xA4;
    public static final int OP_F64_MAX = 0xA5;
    public static final int OP_F64_COPYSIGN = 0xA6;

    // Opcodes - Conversions
    public static final int OP_I32_WRAP_I64 = 0xA7;
    public static final int OP_I32_TRUNC_F32_S = 0xA8;
    public static final int OP_I32_TRUNC_F32_U = 0xA9;
    public static final int OP_I32_TRUNC_F64_S = 0xAA;
    public static final int OP_I32_TRUNC_F64_U = 0xAB;
    public static final int OP_I64_EXTEND_I32_S = 0xAC;
    public static final int OP_I64_EXTEND_I32_U = 0xAD;
    public static final int OP_I64_TRUNC_F32_S = 0xAE;
    public static final int OP_I64_TRUNC_F32_U = 0xAF;
    public static final int OP_I64_TRUNC_F64_S = 0xB0;
    public static final int OP_I64_TRUNC_F64_U = 0xB1;
    public static final int OP_F32_CONVERT_I32_S = 0xB2;
    public static final int OP_F32_CONVERT_I32_U = 0xB3;
    public static final int OP_F32_CONVERT_I64_S = 0xB4;
    public static final int OP_F32_CONVERT_I64_U = 0xB5;
    public static final int OP_F32_DEMOTE_F64 = 0xB6;
    public static final int OP_F64_CONVERT_I32_S = 0xB7;
    public static final int OP_F64_CONVERT_I32_U = 0xB8;
    public static final int OP_F64_CONVERT_I64_S = 0xB9;
    public static final int OP_F64_CONVERT_I64_U = 0xBA;
    public static final int OP_F64_PROMOTE_F32 = 0xBB;
    public static final int OP_I32_REINTERPRET_F32 = 0xBC;
    public static final int OP_I64_REINTERPRET_F64 = 0xBD;
    public static final int OP_F32_REINTERPRET_I32 = 0xBE;
    public static final int OP_F64_REINTERPRET_I64 = 0xBF;

    // Block type encoding
    public static final int BLOCK_TYPE_VOID = 0x40;
}

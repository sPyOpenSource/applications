package jCPU.Wasm;

public class WasmInstruction {
    public final int opcode;
    public final int immI32;
    public final long immI64;
    public final float immF32;
    public final double immF64;
    public final int length;
    public final int[] brTable;

    private WasmInstruction(int opcode, int immI32, long immI64, float immF32, double immF64,
                            int length, int[] brTable) {
        this.opcode = opcode;
        this.immI32 = immI32;
        this.immI64 = immI64;
        this.immF32 = immF32;
        this.immF64 = immF64;
        this.length = length;
        this.brTable = brTable;
    }

    public static WasmInstruction decode(byte[] code, int offset) {
        int pos = offset;
        int opcode = code[pos++] & 0xFF;

        int immI32 = 0;
        long immI64 = 0;
        float immF32 = 0;
        double immF64 = 0;
        int[] brTable = null;

        switch (opcode) {
            case WasmConstants.OP_BLOCK:
            case WasmConstants.OP_LOOP:
            case WasmConstants.OP_IF: {
                int bt = readBlockType(code, pos);
                immI32 = bt;
                pos += blockTypeLength(code, pos);
                break;
            }
            case WasmConstants.OP_ELSE:
            case WasmConstants.OP_END:
            case WasmConstants.OP_NOP:
            case WasmConstants.OP_RETURN:
            case WasmConstants.OP_UNREACHABLE:
            case WasmConstants.OP_DROP:
            case WasmConstants.OP_SELECT:
            case WasmConstants.OP_I32_WRAP_I64:
            case WasmConstants.OP_I64_EXTEND_I32_S:
            case WasmConstants.OP_I64_EXTEND_I32_U:
                break;
            case WasmConstants.OP_BR:
            case WasmConstants.OP_BR_IF: {
                int[] labelIdx = readVarUInt(code, pos);
                immI32 = labelIdx[0];
                pos += labelIdx[1];
                break;
            }
            case WasmConstants.OP_BR_TABLE: {
                int[] targetCount = readVarUInt(code, pos);
                pos += targetCount[1];
                int count = targetCount[0];
                brTable = new int[count + 1];
                for (int i = 0; i <= count; i++) {
                    int[] t = readVarUInt(code, pos);
                    brTable[i] = t[0];
                    pos += t[1];
                }
                break;
            }
            case WasmConstants.OP_CALL: {
                int[] funcIdx = readVarUInt(code, pos);
                immI32 = funcIdx[0];
                pos += funcIdx[1];
                break;
            }
            case WasmConstants.OP_CALL_INDIRECT: {
                int[] typeIdx = readVarUInt(code, pos);
                immI32 = typeIdx[0];
                pos += typeIdx[1];
                int[] tableIdx = readVarUInt(code, pos);
                pos += tableIdx[1];
                break;
            }
            case WasmConstants.OP_LOCAL_GET:
            case WasmConstants.OP_LOCAL_SET:
            case WasmConstants.OP_LOCAL_TEE:
            case WasmConstants.OP_GLOBAL_GET:
            case WasmConstants.OP_GLOBAL_SET: {
                int[] idx = readVarUInt(code, pos);
                immI32 = idx[0];
                pos += idx[1];
                break;
            }
            case WasmConstants.OP_I32_CONST: {
                int[] val = readVarInt(code, pos);
                immI32 = val[0];
                pos += val[1];
                break;
            }
            case WasmConstants.OP_I64_CONST: {
                long[] val = readVarLong(code, pos);
                immI64 = val[0];
                pos += (int) val[1];
                break;
            }
            case WasmConstants.OP_F32_CONST: {
                immF32 = readFloat(code, pos);
                pos += 4;
                break;
            }
            case WasmConstants.OP_F64_CONST: {
                immF64 = readDouble(code, pos);
                pos += 8;
                break;
            }
            case WasmConstants.OP_I32_LOAD:
            case WasmConstants.OP_I64_LOAD:
            case WasmConstants.OP_F32_LOAD:
            case WasmConstants.OP_F64_LOAD:
            case WasmConstants.OP_I32_STORE:
            case WasmConstants.OP_I64_STORE:
            case WasmConstants.OP_F32_STORE:
            case WasmConstants.OP_F64_STORE:
                pos++;
                immI32 = readVarUInt(code, pos)[0];
                pos += readVarUInt(code, pos)[1];
                break;
            case WasmConstants.OP_I32_LOAD8_S:
            case WasmConstants.OP_I32_LOAD8_U:
            case WasmConstants.OP_I32_LOAD16_S:
            case WasmConstants.OP_I32_LOAD16_U:
            case WasmConstants.OP_I64_LOAD8_S:
            case WasmConstants.OP_I64_LOAD8_U:
            case WasmConstants.OP_I64_LOAD16_S:
            case WasmConstants.OP_I64_LOAD16_U:
            case WasmConstants.OP_I64_LOAD32_S:
            case WasmConstants.OP_I64_LOAD32_U:
            case WasmConstants.OP_I32_STORE8:
            case WasmConstants.OP_I32_STORE16:
            case WasmConstants.OP_I64_STORE8:
            case WasmConstants.OP_I64_STORE16:
            case WasmConstants.OP_I64_STORE32:
                pos++;
                immI32 = readVarUInt(code, pos)[0];
                pos += readVarUInt(code, pos)[1];
                break;
            case WasmConstants.OP_MEMORY_SIZE:
            case WasmConstants.OP_MEMORY_GROW:
                pos++;
                break;
            default:
                break;
        }

        int length = pos - offset;
        return new WasmInstruction(opcode, immI32, immI64, immF32, immF64, length, brTable);
    }

    public static int[] readVarUInt(byte[] data, int pos) {
        int result = 0;
        int shift = 0;
        int bytesRead = 0;
        while (true) {
            int byte_ = data[pos++] & 0xFF;
            result |= (byte_ & 0x7F) << shift;
            bytesRead++;
            if ((byte_ & 0x80) == 0) break;
            shift += 7;
        }
        return new int[]{result, bytesRead};
    }

    public static int[] readVarInt(byte[] data, int pos) {
        int result = 0;
        int shift = 0;
        int bytesRead = 0;
        int byte_ = 0;
        while (true) {
            byte_ = data[pos++] & 0xFF;
            result |= (byte_ & 0x7F) << shift;
            bytesRead++;
            shift += 7;
            if ((byte_ & 0x80) == 0) break;
        }
        if (shift < 32 && (byte_ & 0x40) != 0) {
            result |= (~0 << shift);
        }
        return new int[]{result, bytesRead};
    }

    public static long[] readVarLong(byte[] data, int pos) {
        long result = 0;
        int shift = 0;
        int bytesRead = 0;
        int byte_ = 0;
        while (true) {
            byte_ = data[pos++] & 0xFF;
            result |= (long) (byte_ & 0x7F) << shift;
            bytesRead++;
            shift += 7;
            if ((byte_ & 0x80) == 0) break;
        }
        if (shift < 64 && (byte_ & 0x40) != 0) {
            result |= (~0L << shift);
        }
        return new long[]{result, bytesRead};
    }

    public static float readFloat(byte[] data, int pos) {
        int bits = (data[pos] & 0xFF)
                 | ((data[pos + 1] & 0xFF) << 8)
                 | ((data[pos + 2] & 0xFF) << 16)
                 | ((data[pos + 3] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    public static double readDouble(byte[] data, int pos) {
        long bits = (data[pos] & 0xFFL)
                  | ((data[pos + 1] & 0xFFL) << 8)
                  | ((data[pos + 2] & 0xFFL) << 16)
                  | ((data[pos + 3] & 0xFFL) << 24)
                  | ((data[pos + 4] & 0xFFL) << 32)
                  | ((data[pos + 5] & 0xFFL) << 40)
                  | ((data[pos + 6] & 0xFFL) << 48)
                  | ((data[pos + 7] & 0xFFL) << 56);
        return Double.longBitsToDouble(bits);
    }

    private static int readBlockType(byte[] code, int pos) {
        if (code[pos] == WasmConstants.BLOCK_TYPE_VOID) return WasmConstants.BLOCK_TYPE_VOID;
        int[] r = readVarUInt(code, pos);
        return r[0];
    }

    private static int blockTypeLength(byte[] code, int pos) {
        if (code[pos] == WasmConstants.BLOCK_TYPE_VOID) return 1;
        int[] r = readVarUInt(code, pos);
        return r[1];
    }

    public String toString() {
        return String.format("WasmInst[opcode=0x%02X, imm=0x%X, len=%d]", opcode, immI32, length);
    }
}

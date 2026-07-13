package jCPU.Wasm;

import jCPU.MCS51.CPU;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class WasmSim extends CPU {
    private WasmModule module;
    private WasmRuntime runtime;
    private boolean running = false;
    private WasmStack operandStack;
    private long[] locals = new long[256];

    public WasmSim() {
        System.out.println("WebAssembly Simulator");
    }

    public void loadWasm(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        module = WasmModule.parse(bytes);
        System.out.println(module);

        int memMax = module.memoryMax >= 0 ? module.memoryMax : 65535;
        runtime = new WasmRuntime(module.memoryMin, memMax, module.globalSection.size());

        for (int i = 0; i < module.globalSection.size(); i++) {
            runtime.globals[i] = module.globalSection.get(i)[2];
        }

        for (WasmModule.DataEntry data : module.dataSection) {
            int offset = evaluateInitExpr(data.offsetExpr);
            runtime.memory.loadData(offset, data.data);
        }

        if (module.startFunction >= 0) {
            runtime.pc = getFuncBodyOffset(module.startFunction);
        } else {
            WasmModule.ExportEntry start = module.exportSection.get("_start");
            if (start != null) {
                runtime.pc = getFuncBodyOffset(start.index);
            }
        }

        operandStack = runtime.stack;
    }

    private int evaluateInitExpr(byte[] expr) {
        if (expr.length == 0) return 0;
        int op = expr[0] & 0xFF;
        if (op == WasmConstants.OP_I32_CONST) {
            return WasmInstruction.readVarInt(expr, 1)[0];
        }
        return 0;
    }

    private int getFuncBodyOffset(int funcIdx) {
        int offset = 0;
        for (int i = 0; i < funcIdx && i < module.codeSection.size(); i++) {
            byte[] body = module.codeSection.get(i);
            int sizeBytes = WasmInstruction.readVarUInt(body, 0)[1];
            offset += body.length;
        }
        if (funcIdx < module.codeSection.size()) {
            byte[] body = module.codeSection.get(funcIdx);
            int sizeBytes = WasmInstruction.readVarUInt(body, 0)[1];
            return offset + sizeBytes;
        }
        return offset;
    }

    @Override
    public void go(int limit) throws Exception {
        running = true;
        for (int i = 0; i < limit && running; i++) {
            if (step() == 0) break;
        }
        running = false;
    }

    @Override
    public int step() {
        if (module == null || runtime == null) return 0;
        try {
            byte[] code = getCurrentCode();
            if (runtime.pc >= code.length) return 0;

            WasmInstruction inst = WasmInstruction.decode(code, runtime.pc);
            executeInstruction(inst, code);
            runtime.pc += inst.length;
            return 1;
        } catch (Exception e) {
            System.out.println("WASM error at pc=" + runtime.pc + ": " + e.getMessage());
            return 0;
        }
    }

    private byte[] getCurrentCode() {
        int totalSize = 0;
        for (byte[] body : module.codeSection) {
            totalSize += body.length;
        }
        byte[] allCode = new byte[totalSize];
        int pos = 0;
        for (byte[] body : module.codeSection) {
            System.arraycopy(body, 0, allCode, pos, body.length);
            pos += body.length;
        }
        return allCode;
    }

    @Override
    public String getDecodeAt(int pc) {
        if (module == null) return "";
        try {
            byte[] code = getCurrentCode();
            if (pc >= code.length) return "";
            WasmInstruction inst = WasmInstruction.decode(code, pc);
            return describeInstruction(inst);
        } catch (Exception e) {
            return "???";
        }
    }

    private String describeInstruction(WasmInstruction inst) {
        switch (inst.opcode) {
            case WasmConstants.OP_BLOCK: return "block";
            case WasmConstants.OP_LOOP: return "loop";
            case WasmConstants.OP_IF: return "if";
            case WasmConstants.OP_ELSE: return "else";
            case WasmConstants.OP_END: return "end";
            case WasmConstants.OP_BR: return "br $" + inst.immI32;
            case WasmConstants.OP_BR_IF: return "br_if $" + inst.immI32;
            case WasmConstants.OP_CALL: return "call $" + inst.immI32;
            case WasmConstants.OP_RETURN: return "return";
            case WasmConstants.OP_DROP: return "drop";
            case WasmConstants.OP_SELECT: return "select";
            case WasmConstants.OP_LOCAL_GET: return "local.get $" + inst.immI32;
            case WasmConstants.OP_LOCAL_SET: return "local.set $" + inst.immI32;
            case WasmConstants.OP_LOCAL_TEE: return "local.tee $" + inst.immI32;
            case WasmConstants.OP_GLOBAL_GET: return "global.get $" + inst.immI32;
            case WasmConstants.OP_GLOBAL_SET: return "global.set $" + inst.immI32;
            case WasmConstants.OP_I32_CONST: return "i32.const " + inst.immI32;
            case WasmConstants.OP_I64_CONST: return "i64.const " + inst.immI64;
            case WasmConstants.OP_I32_ADD: return "i32.add";
            case WasmConstants.OP_I32_SUB: return "i32.sub";
            case WasmConstants.OP_I32_MUL: return "i32.mul";
            case WasmConstants.OP_I32_DIV_S: return "i32.div_s";
            case WasmConstants.OP_I32_DIV_U: return "i32.div_u";
            case WasmConstants.OP_I32_REM_S: return "i32.rem_s";
            case WasmConstants.OP_I32_REM_U: return "i32.rem_u";
            case WasmConstants.OP_I32_AND: return "i32.and";
            case WasmConstants.OP_I32_OR: return "i32.or";
            case WasmConstants.OP_I32_XOR: return "i32.xor";
            case WasmConstants.OP_I32_SHL: return "i32.shl";
            case WasmConstants.OP_I32_SHR_S: return "i32.shr_s";
            case WasmConstants.OP_I32_SHR_U: return "i32.shr_u";
            case WasmConstants.OP_I32_ROTR: return "i32.rotr";
            case WasmConstants.OP_I32_ROTL: return "i32.rotl";
            case WasmConstants.OP_I32_CLZ: return "i32.clz";
            case WasmConstants.OP_I32_CTZ: return "i32.ctz";
            case WasmConstants.OP_I32_POPCNT: return "i32.popcnt";
            case WasmConstants.OP_I32_EQZ: return "i32.eqz";
            case WasmConstants.OP_I32_EQ: return "i32.eq";
            case WasmConstants.OP_I32_NE: return "i32.ne";
            case WasmConstants.OP_I32_LT_S: return "i32.lt_s";
            case WasmConstants.OP_I32_LT_U: return "i32.lt_u";
            case WasmConstants.OP_I32_GT_S: return "i32.gt_s";
            case WasmConstants.OP_I32_GT_U: return "i32.gt_u";
            case WasmConstants.OP_I32_LE_S: return "i32.le_s";
            case WasmConstants.OP_I32_LE_U: return "i32.le_u";
            case WasmConstants.OP_I32_GE_S: return "i32.ge_s";
            case WasmConstants.OP_I32_GE_U: return "i32.ge_u";
            case WasmConstants.OP_I64_ADD: return "i64.add";
            case WasmConstants.OP_I64_SUB: return "i64.sub";
            case WasmConstants.OP_I64_MUL: return "i64.mul";
            case WasmConstants.OP_I32_LOAD: return "i32.load offset=" + inst.immI32;
            case WasmConstants.OP_I64_LOAD: return "i64.load offset=" + inst.immI32;
            case WasmConstants.OP_I32_LOAD8_S: return "i32.load8_s offset=" + inst.immI32;
            case WasmConstants.OP_I32_LOAD8_U: return "i32.load8_u offset=" + inst.immI32;
            case WasmConstants.OP_I32_LOAD16_S: return "i32.load16_s offset=" + inst.immI32;
            case WasmConstants.OP_I32_LOAD16_U: return "i32.load16_u offset=" + inst.immI32;
            case WasmConstants.OP_I32_STORE: return "i32.store offset=" + inst.immI32;
            case WasmConstants.OP_I64_STORE: return "i64.store offset=" + inst.immI32;
            case WasmConstants.OP_I32_STORE8: return "i32.store8 offset=" + inst.immI32;
            case WasmConstants.OP_I32_STORE16: return "i32.store16 offset=" + inst.immI32;
            case WasmConstants.OP_MEMORY_SIZE: return "memory.size";
            case WasmConstants.OP_MEMORY_GROW: return "memory.grow";
            case WasmConstants.OP_NOP: return "nop";
            case WasmConstants.OP_UNREACHABLE: return "unreachable";
            default: return "opcode=0x" + Integer.toHexString(inst.opcode);
        }
    }

    private void executeInstruction(WasmInstruction inst, byte[] code) {
        switch (inst.opcode) {
            case WasmConstants.OP_NOP:
            case WasmConstants.OP_END:
            case WasmConstants.OP_ELSE:
                break;
            case WasmConstants.OP_UNREACHABLE:
                throw new RuntimeException("unreachable");
            case WasmConstants.OP_RETURN:
                break;
            case WasmConstants.OP_CALL:
                executeCall(inst.immI32);
                break;

            case WasmConstants.OP_DROP:
                operandStack.pop();
                break;
            case WasmConstants.OP_SELECT: {
                long c = operandStack.pop();
                long v2 = operandStack.pop();
                long v1 = operandStack.pop();
                operandStack.push(c != 0 ? v1 : v2);
                break;
            }

            case WasmConstants.OP_LOCAL_GET:
                operandStack.push(getLocal(inst.immI32));
                break;
            case WasmConstants.OP_LOCAL_SET:
                setLocal(inst.immI32, operandStack.pop());
                break;
            case WasmConstants.OP_LOCAL_TEE: {
                long val = operandStack.peek();
                setLocal(inst.immI32, val);
                break;
            }
            case WasmConstants.OP_GLOBAL_GET:
                operandStack.push(runtime.globals[inst.immI32]);
                break;
            case WasmConstants.OP_GLOBAL_SET:
                runtime.globals[inst.immI32] = operandStack.pop();
                break;

            case WasmConstants.OP_I32_LOAD: {
                int base = (int) operandStack.pop();
                operandStack.push(runtime.memory.readI32(base + inst.immI32) & 0xFFFFFFFFL);
                break;
            }
            case WasmConstants.OP_I64_LOAD: {
                int base = (int) operandStack.pop();
                operandStack.push(runtime.memory.readI64(base + inst.immI32));
                break;
            }
            case WasmConstants.OP_F32_LOAD: {
                int base = (int) operandStack.pop();
                operandStack.push(Float.floatToIntBits(runtime.memory.readF32(base + inst.immI32)));
                break;
            }
            case WasmConstants.OP_F64_LOAD: {
                int base = (int) operandStack.pop();
                operandStack.push(Double.doubleToRawLongBits(runtime.memory.readF64(base + inst.immI32)));
                break;
            }
            case WasmConstants.OP_I32_LOAD8_S: {
                int base = (int) operandStack.pop();
                operandStack.push(runtime.memory.readByte(base + inst.immI32));
                break;
            }
            case WasmConstants.OP_I32_LOAD8_U: {
                int base = (int) operandStack.pop();
                operandStack.push(runtime.memory.readByte(base + inst.immI32) & 0xFF);
                break;
            }
            case WasmConstants.OP_I32_LOAD16_S: {
                int base = (int) operandStack.pop();
                short s = (short) ((runtime.memory.readByte(base + inst.immI32) & 0xFF)
                                | ((runtime.memory.readByte(base + inst.immI32 + 1) & 0xFF) << 8));
                operandStack.push(s);
                break;
            }
            case WasmConstants.OP_I32_LOAD16_U: {
                int base = (int) operandStack.pop();
                int val = (runtime.memory.readByte(base + inst.immI32) & 0xFF)
                        | ((runtime.memory.readByte(base + inst.immI32 + 1) & 0xFF) << 8);
                operandStack.push(val);
                break;
            }
            case WasmConstants.OP_I32_STORE: {
                int base = (int) operandStack.pop();
                int val = (int) operandStack.pop();
                runtime.memory.writeI32(base + inst.immI32, val);
                break;
            }
            case WasmConstants.OP_I64_STORE: {
                int base = (int) operandStack.pop();
                long val = operandStack.pop();
                runtime.memory.writeI64(base + inst.immI32, val);
                break;
            }
            case WasmConstants.OP_F32_STORE: {
                int base = (int) operandStack.pop();
                float val = Float.intBitsToFloat((int) operandStack.pop());
                runtime.memory.writeF32(base + inst.immI32, val);
                break;
            }
            case WasmConstants.OP_F64_STORE: {
                int base = (int) operandStack.pop();
                double val = Double.longBitsToDouble(operandStack.pop());
                runtime.memory.writeF64(base + inst.immI32, val);
                break;
            }
            case WasmConstants.OP_I32_STORE8: {
                int base = (int) operandStack.pop();
                int val = (int) operandStack.pop();
                runtime.memory.writeByte(base + inst.immI32, (byte) val);
                break;
            }
            case WasmConstants.OP_I32_STORE16: {
                int base = (int) operandStack.pop();
                int val = (int) operandStack.pop();
                runtime.memory.writeByte(base + inst.immI32, (byte) val);
                runtime.memory.writeByte(base + inst.immI32 + 1, (byte) (val >> 8));
                break;
            }
            case WasmConstants.OP_MEMORY_SIZE:
                operandStack.push(runtime.memory.size());
                break;
            case WasmConstants.OP_MEMORY_GROW: {
                int pages = (int) operandStack.pop();
                operandStack.push(runtime.memory.grow(pages));
                break;
            }

            case WasmConstants.OP_I32_CONST:
                operandStack.push(inst.immI32);
                break;
            case WasmConstants.OP_I64_CONST:
                operandStack.push(inst.immI64);
                break;
            case WasmConstants.OP_F32_CONST:
                operandStack.push(Float.floatToIntBits(inst.immF32));
                break;
            case WasmConstants.OP_F64_CONST:
                operandStack.push(Double.doubleToRawLongBits(inst.immF64));
                break;

            case WasmConstants.OP_I32_EQZ:
                operandStack.push(operandStack.pop() == 0 ? 1 : 0);
                break;
            case WasmConstants.OP_I32_EQ:
                operandStack.push(operandStack.pop() == operandStack.pop() ? 1 : 0);
                break;
            case WasmConstants.OP_I32_NE: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a != b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_LT_S: {
                int b = (int) operandStack.pop();
                int a = (int) operandStack.pop();
                operandStack.push(a < b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_LT_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(a < b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_GT_S: {
                int b = (int) operandStack.pop();
                int a = (int) operandStack.pop();
                operandStack.push(a > b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_GT_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(a > b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_LE_S: {
                int b = (int) operandStack.pop();
                int a = (int) operandStack.pop();
                operandStack.push(a <= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_LE_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(a <= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_GE_S: {
                int b = (int) operandStack.pop();
                int a = (int) operandStack.pop();
                operandStack.push(a >= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I32_GE_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(a >= b ? 1 : 0);
                break;
            }

            case WasmConstants.OP_I64_EQZ:
                operandStack.push(operandStack.pop() == 0 ? 1 : 0);
                break;
            case WasmConstants.OP_I64_EQ:
                operandStack.push(operandStack.pop() == operandStack.pop() ? 1 : 0);
                break;
            case WasmConstants.OP_I64_NE: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a != b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_LT_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a < b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_LT_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.compareUnsigned(a, b) < 0 ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_GT_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a > b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_GT_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.compareUnsigned(a, b) > 0 ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_LE_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a <= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_LE_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.compareUnsigned(a, b) <= 0 ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_GE_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a >= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_I64_GE_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.compareUnsigned(a, b) >= 0 ? 1 : 0);
                break;
            }

            case WasmConstants.OP_F32_EQ: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a == b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F32_NE: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a != b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F32_LT: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a < b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F32_GT: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a > b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F32_LE: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a <= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F32_GE: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(a >= b ? 1 : 0);
                break;
            }

            case WasmConstants.OP_F64_EQ: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a == b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F64_NE: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a != b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F64_LT: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a < b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F64_GT: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a > b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F64_LE: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a <= b ? 1 : 0);
                break;
            }
            case WasmConstants.OP_F64_GE: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(a >= b ? 1 : 0);
                break;
            }

            case WasmConstants.OP_I32_ADD:
                operandStack.push(operandStack.pop() + operandStack.pop());
                break;
            case WasmConstants.OP_I32_SUB: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a - b);
                break;
            }
            case WasmConstants.OP_I32_MUL:
                operandStack.push(operandStack.pop() * operandStack.pop());
                break;
            case WasmConstants.OP_I32_DIV_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i32.div_s: division by zero");
                operandStack.push(a / b);
                break;
            }
            case WasmConstants.OP_I32_DIV_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                if (b == 0) throw new RuntimeException("i32.div_u: division by zero");
                operandStack.push(a / b);
                break;
            }
            case WasmConstants.OP_I32_REM_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i32.rem_s: division by zero");
                operandStack.push(a % b);
                break;
            }
            case WasmConstants.OP_I32_REM_U: {
                long b = operandStack.pop() & 0xFFFFFFFFL;
                long a = operandStack.pop() & 0xFFFFFFFFL;
                if (b == 0) throw new RuntimeException("i32.rem_u: division by zero");
                operandStack.push(a % b);
                break;
            }
            case WasmConstants.OP_I32_AND:
                operandStack.push(operandStack.pop() & operandStack.pop());
                break;
            case WasmConstants.OP_I32_OR:
                operandStack.push(operandStack.pop() | operandStack.pop());
                break;
            case WasmConstants.OP_I32_XOR:
                operandStack.push(operandStack.pop() ^ operandStack.pop());
                break;
            case WasmConstants.OP_I32_SHL: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a << (b & 0x1F));
                break;
            }
            case WasmConstants.OP_I32_SHR_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push((int) a >> (b & 0x1F));
                break;
            }
            case WasmConstants.OP_I32_SHR_U: {
                long b = operandStack.pop();
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(a >>> (b & 0x1F));
                break;
            }
            case WasmConstants.OP_I32_ROTR: {
                long b = operandStack.pop();
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Integer.rotateRight((int) a, (int) b));
                break;
            }
            case WasmConstants.OP_I32_ROTL: {
                long b = operandStack.pop();
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Integer.rotateLeft((int) a, (int) b));
                break;
            }
            case WasmConstants.OP_I32_CLZ: {
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Integer.numberOfLeadingZeros((int) a));
                break;
            }
            case WasmConstants.OP_I32_CTZ: {
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Integer.numberOfTrailingZeros((int) a));
                break;
            }
            case WasmConstants.OP_I32_POPCNT: {
                long a = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Integer.bitCount((int) a));
                break;
            }

            case WasmConstants.OP_I64_ADD:
                operandStack.push(operandStack.pop() + operandStack.pop());
                break;
            case WasmConstants.OP_I64_SUB: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a - b);
                break;
            }
            case WasmConstants.OP_I64_MUL:
                operandStack.push(operandStack.pop() * operandStack.pop());
                break;
            case WasmConstants.OP_I64_DIV_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i64.div_s: division by zero");
                operandStack.push(a / b);
                break;
            }
            case WasmConstants.OP_I64_DIV_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i64.div_u: division by zero");
                operandStack.push(Long.divideUnsigned(a, b));
                break;
            }
            case WasmConstants.OP_I64_REM_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i64.rem_s: division by zero");
                operandStack.push(a % b);
                break;
            }
            case WasmConstants.OP_I64_REM_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                if (b == 0) throw new RuntimeException("i64.rem_u: division by zero");
                operandStack.push(Long.remainderUnsigned(a, b));
                break;
            }
            case WasmConstants.OP_I64_AND:
                operandStack.push(operandStack.pop() & operandStack.pop());
                break;
            case WasmConstants.OP_I64_OR:
                operandStack.push(operandStack.pop() | operandStack.pop());
                break;
            case WasmConstants.OP_I64_XOR:
                operandStack.push(operandStack.pop() ^ operandStack.pop());
                break;
            case WasmConstants.OP_I64_SHL: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a << (b & 0x3F));
                break;
            }
            case WasmConstants.OP_I64_SHR_S: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a >> (b & 0x3F));
                break;
            }
            case WasmConstants.OP_I64_SHR_U: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(a >>> (b & 0x3F));
                break;
            }
            case WasmConstants.OP_I64_ROTR: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.rotateRight(a, (int) b));
                break;
            }
            case WasmConstants.OP_I64_ROTL: {
                long b = operandStack.pop();
                long a = operandStack.pop();
                operandStack.push(Long.rotateLeft(a, (int) b));
                break;
            }
            case WasmConstants.OP_I64_CLZ: {
                long a = operandStack.pop();
                operandStack.push(Long.numberOfLeadingZeros(a));
                break;
            }
            case WasmConstants.OP_I64_CTZ: {
                long a = operandStack.pop();
                operandStack.push(Long.numberOfTrailingZeros(a));
                break;
            }
            case WasmConstants.OP_I64_POPCNT: {
                long a = operandStack.pop();
                operandStack.push(Long.bitCount(a));
                break;
            }

            case WasmConstants.OP_F32_ADD: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(a + b));
                break;
            }
            case WasmConstants.OP_F32_SUB: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(a - b));
                break;
            }
            case WasmConstants.OP_F32_MUL: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(a * b));
                break;
            }
            case WasmConstants.OP_F32_DIV: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(a / b));
                break;
            }
            case WasmConstants.OP_F32_ABS: {
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(Math.abs(a)));
                break;
            }
            case WasmConstants.OP_F32_NEG: {
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(-a));
                break;
            }
            case WasmConstants.OP_F32_SQRT: {
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits((float) Math.sqrt(a)));
                break;
            }
            case WasmConstants.OP_F32_MIN: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(Math.min(a, b)));
                break;
            }
            case WasmConstants.OP_F32_MAX: {
                float b = Float.intBitsToFloat((int) operandStack.pop());
                float a = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Float.floatToIntBits(Math.max(a, b)));
                break;
            }

            case WasmConstants.OP_F64_ADD: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(a + b));
                break;
            }
            case WasmConstants.OP_F64_SUB: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(a - b));
                break;
            }
            case WasmConstants.OP_F64_MUL: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(a * b));
                break;
            }
            case WasmConstants.OP_F64_DIV: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(a / b));
                break;
            }
            case WasmConstants.OP_F64_ABS: {
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(Math.abs(a)));
                break;
            }
            case WasmConstants.OP_F64_NEG: {
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(-a));
                break;
            }
            case WasmConstants.OP_F64_SQRT: {
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(Math.sqrt(a)));
                break;
            }
            case WasmConstants.OP_F64_MIN: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(Math.min(a, b)));
                break;
            }
            case WasmConstants.OP_F64_MAX: {
                double b = Double.longBitsToDouble(operandStack.pop());
                double a = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits(Math.max(a, b)));
                break;
            }

            case WasmConstants.OP_I32_WRAP_I64:
                operandStack.push((int) operandStack.pop());
                break;
            case WasmConstants.OP_I64_EXTEND_I32_S:
                operandStack.push((int) operandStack.pop());
                break;
            case WasmConstants.OP_I64_EXTEND_I32_U:
                operandStack.push(operandStack.pop() & 0xFFFFFFFFL);
                break;
            case WasmConstants.OP_F32_CONVERT_I32_S: {
                int val = (int) operandStack.pop();
                operandStack.push(Float.floatToIntBits((float) val));
                break;
            }
            case WasmConstants.OP_F32_CONVERT_I32_U: {
                long val = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Float.floatToIntBits((float) val));
                break;
            }
            case WasmConstants.OP_F32_CONVERT_I64_S: {
                long val = operandStack.pop();
                operandStack.push(Float.floatToIntBits((float) val));
                break;
            }
            case WasmConstants.OP_F32_CONVERT_I64_U: {
                long val = operandStack.pop();
                operandStack.push(Float.floatToIntBits((float) Long.toUnsignedString(val).charAt(0)));
                break;
            }
            case WasmConstants.OP_F32_DEMOTE_F64: {
                double val = Double.longBitsToDouble(operandStack.pop());
                operandStack.push(Float.floatToIntBits((float) val));
                break;
            }
            case WasmConstants.OP_F64_CONVERT_I32_S: {
                int val = (int) operandStack.pop();
                operandStack.push(Double.doubleToRawLongBits((double) val));
                break;
            }
            case WasmConstants.OP_F64_CONVERT_I32_U: {
                long val = operandStack.pop() & 0xFFFFFFFFL;
                operandStack.push(Double.doubleToRawLongBits((double) val));
                break;
            }
            case WasmConstants.OP_F64_CONVERT_I64_S: {
                long val = operandStack.pop();
                operandStack.push(Double.doubleToRawLongBits((double) val));
                break;
            }
            case WasmConstants.OP_F64_CONVERT_I64_U: {
                long val = operandStack.pop();
                operandStack.push(Double.doubleToRawLongBits((double) Long.toUnsignedString(val).charAt(0)));
                break;
            }
            case WasmConstants.OP_F64_PROMOTE_F32: {
                float val = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push(Double.doubleToRawLongBits((double) val));
                break;
            }
            case WasmConstants.OP_I32_TRUNC_F32_S: {
                float val = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push((int) val);
                break;
            }
            case WasmConstants.OP_I32_TRUNC_F32_U: {
                float val = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push((int) val);
                break;
            }
            case WasmConstants.OP_I32_TRUNC_F64_S: {
                double val = Double.longBitsToDouble(operandStack.pop());
                operandStack.push((int) val);
                break;
            }
            case WasmConstants.OP_I32_TRUNC_F64_U: {
                double val = Double.longBitsToDouble(operandStack.pop());
                operandStack.push((int) val);
                break;
            }
            case WasmConstants.OP_I64_TRUNC_F32_S: {
                float val = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push((long) val);
                break;
            }
            case WasmConstants.OP_I64_TRUNC_F32_U: {
                float val = Float.intBitsToFloat((int) operandStack.pop());
                operandStack.push((long) val);
                break;
            }
            case WasmConstants.OP_I64_TRUNC_F64_S: {
                double val = Double.longBitsToDouble(operandStack.pop());
                operandStack.push((long) val);
                break;
            }
            case WasmConstants.OP_I64_TRUNC_F64_U: {
                double val = Double.longBitsToDouble(operandStack.pop());
                operandStack.push((long) val);
                break;
            }

            case WasmConstants.OP_I32_REINTERPRET_F32:
            case WasmConstants.OP_I64_REINTERPRET_F64:
            case WasmConstants.OP_F32_REINTERPRET_I32:
            case WasmConstants.OP_F64_REINTERPRET_I64:
                break;

            default:
                System.out.println("Unimplemented opcode: 0x" + Integer.toHexString(inst.opcode));
                break;
        }
    }

    private long getLocal(int idx) {
        if (idx >= 0 && idx < locals.length) return locals[idx];
        return 0;
    }

    private void setLocal(int idx, long val) {
        if (idx >= locals.length) {
            long[] newLocals = new long[idx + 64];
            System.arraycopy(locals, 0, newLocals, 0, locals.length);
            locals = newLocals;
        }
        locals[idx] = val;
    }

    private void executeCall(int funcIdx) {
        if (funcIdx >= module.codeSection.size()) return;
        runtime.pushCall(runtime.pc);
        runtime.pc = getFuncBodyOffset(funcIdx);
        Arrays.fill(locals, 0);
    }

    @Override
    public String toString() {
        if (module != null) return module.toString();
        return "WebAssembly Simulator (no module loaded)";
    }

    public WasmModule getModule() { return module; }
    public WasmRuntime getRuntime() { return runtime; }
    public WasmStack getStack() { return operandStack; }
}

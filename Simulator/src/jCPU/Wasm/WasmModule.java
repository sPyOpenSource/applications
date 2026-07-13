package jCPU.Wasm;

import java.io.*;
import java.util.*;

public class WasmModule {
    public static final int WASM_MAGIC = 0x0061736D;
    public static final int WASM_VERSION = 0x01000000;

    public List<int[]> typeSection = new ArrayList<>();
    public List<Integer> functionSection = new ArrayList<>();
    public int memoryMin = 0;
    public int memoryMax = -1;
    public List<long[]> globalSection = new ArrayList<>();
    public Map<String, ExportEntry> exportSection = new LinkedHashMap<>();
    public List<byte[]> codeSection = new ArrayList<>();
    public List<DataEntry> dataSection = new ArrayList<>();
    public int startFunction = -1;

    public static class ExportEntry {
        public final String name;
        public final int kind;
        public final int index;
        public ExportEntry(String name, int kind, int index) {
            this.name = name;
            this.kind = kind;
            this.index = index;
        }
    }

    public static class DataEntry {
        public final int memoryIndex;
        public final byte[] offsetExpr;
        public final byte[] data;
        public DataEntry(int memIdx, byte[] offset, byte[] data) {
            this.memoryIndex = memIdx;
            this.offsetExpr = offset;
            this.data = data;
        }
    }

    public static WasmModule parse(byte[] bytes) throws IOException {
        WasmModule module = new WasmModule();
        int pos = 0;

        int magic = readU32(bytes, pos); pos += 4;
        int version = readU32(bytes, pos); pos += 4;
        if (magic != WASM_MAGIC) throw new IOException("Invalid WASM magic: 0x" + Integer.toHexString(magic));
        if (version != WASM_VERSION) throw new IOException("Unsupported WASM version: " + version);

        while (pos < bytes.length) {
            int sectionId = bytes[pos++] & 0xFF;
            int[] sizeResult = WasmInstruction.readVarUInt(bytes, pos);
            int sectionSize = sizeResult[0];
            pos += sizeResult[1];
            int sectionStart = pos;

            switch (sectionId) {
                case WasmConstants.SECTION_TYPE:
                    parseTypeSection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_FUNCTION:
                    parseFunctionSection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_MEMORY:
                    parseMemorySection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_GLOBAL:
                    parseGlobalSection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_EXPORT:
                    parseExportSection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_START: {
                    int[] startIdx = WasmInstruction.readVarUInt(bytes, pos);
                    module.startFunction = startIdx[0];
                    break;
                }
                case WasmConstants.SECTION_CODE:
                    parseCodeSection(module, bytes, pos, sectionSize);
                    break;
                case WasmConstants.SECTION_DATA:
                    parseDataSection(module, bytes, pos, sectionSize);
                    break;
                default:
                    break;
            }

            pos = sectionStart + sectionSize;
        }

        return module;
    }

    private static void parseTypeSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            pos++; // 0x60 = func type
            int paramCount = WasmInstruction.readVarUInt(bytes, pos)[0];
            pos += WasmInstruction.readVarUInt(bytes, pos)[1];
            pos += paramCount; // skip param types
            int retCount = WasmInstruction.readVarUInt(bytes, pos)[0];
            pos += WasmInstruction.readVarUInt(bytes, pos)[1];
            int retType = WasmConstants.TYPE_VOID;
            if (retCount > 0) {
                retType = bytes[pos] & 0xFF;
                pos++;
            }
            module.typeSection.add(new int[]{paramCount, retType});
        }
    }

    private static void parseFunctionSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            int[] typeIdx = WasmInstruction.readVarUInt(bytes, pos);
            module.functionSection.add(typeIdx[0]);
            pos += typeIdx[1];
        }
    }

    private static void parseMemorySection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        if (count > 0) {
            int flags = bytes[pos++] & 0xFF;
            int[] min = WasmInstruction.readVarUInt(bytes, pos);
            pos += min[1];
            module.memoryMin = min[0];
            if ((flags & 1) != 0) {
                int[] max = WasmInstruction.readVarUInt(bytes, pos);
                pos += max[1];
                module.memoryMax = max[0];
            }
        }
    }

    private static void parseGlobalSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            int type = bytes[pos++] & 0xFF;
            int mutability = bytes[pos++] & 0xFF;
            int initOp = bytes[pos++] & 0xFF;
            long initValue = 0;
            if (initOp == WasmConstants.OP_I32_CONST) {
                int[] v = WasmInstruction.readVarInt(bytes, pos);
                initValue = v[0];
                pos += v[1];
            } else if (initOp == WasmConstants.OP_I64_CONST) {
                long[] v = WasmInstruction.readVarLong(bytes, pos);
                initValue = v[0];
                pos += (int) v[1];
            }
            pos++; // END opcode
            module.globalSection.add(new long[]{type, mutability, initValue});
        }
    }

    private static void parseExportSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            int nameLen = WasmInstruction.readVarUInt(bytes, pos)[0];
            pos += WasmInstruction.readVarUInt(bytes, pos)[1];
            String name = new String(bytes, pos, nameLen);
            pos += nameLen;
            int kind = bytes[pos++] & 0xFF;
            int[] idx = WasmInstruction.readVarUInt(bytes, pos);
            pos += idx[1];
            module.exportSection.put(name, new ExportEntry(name, kind, idx[0]));
        }
    }

    private static void parseCodeSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            int[] bodySize = WasmInstruction.readVarUInt(bytes, pos);
            pos += bodySize[1];
            byte[] body = new byte[bodySize[0]];
            System.arraycopy(bytes, pos, body, 0, bodySize[0]);
            module.codeSection.add(body);
            pos += bodySize[0];
        }
    }

    private static void parseDataSection(WasmModule module, byte[] bytes, int start, int size) {
        int pos = start;
        int count = WasmInstruction.readVarUInt(bytes, pos)[0];
        pos += WasmInstruction.readVarUInt(bytes, pos)[1];
        for (int i = 0; i < count; i++) {
            int memIdx = WasmInstruction.readVarUInt(bytes, pos)[0];
            pos += WasmInstruction.readVarUInt(bytes, pos)[1];
            List<Byte> initExpr = new ArrayList<>();
            while (bytes[pos] != WasmConstants.OP_END) {
                initExpr.add(bytes[pos++]);
            }
            pos++;
            byte[] initArr = new byte[initExpr.size()];
            for (int j = 0; j < initArr.length; j++) initArr[j] = initExpr.get(j);
            int dataLen = WasmInstruction.readVarUInt(bytes, pos)[0];
            pos += WasmInstruction.readVarUInt(bytes, pos)[1];
            byte[] data = new byte[dataLen];
            System.arraycopy(bytes, pos, data, 0, dataLen);
            pos += dataLen;
            module.dataSection.add(new DataEntry(memIdx, initArr, data));
        }
    }

    private static int readU32(byte[] bytes, int pos) {
        return (bytes[pos] & 0xFF)
             | ((bytes[pos + 1] & 0xFF) << 8)
             | ((bytes[pos + 2] & 0xFF) << 16)
             | ((bytes[pos + 3] & 0xFF) << 24);
    }

    public int getLocalCount(int funcIdx) {
        if (funcIdx >= functionSection.size()) return 0;
        int typeIdx = functionSection.get(funcIdx);
        if (typeIdx >= typeSection.size()) return 0;
        return typeSection.get(typeIdx)[0];
    }

    public int getReturnType(int funcIdx) {
        if (funcIdx >= functionSection.size()) return WasmConstants.TYPE_VOID;
        int typeIdx = functionSection.get(funcIdx);
        if (typeIdx >= typeSection.size()) return WasmConstants.TYPE_VOID;
        return typeSection.get(typeIdx)[1];
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WASM Module:\n");
        sb.append("  Types: ").append(typeSection.size()).append("\n");
        sb.append("  Functions: ").append(functionSection.size()).append("\n");
        sb.append("  Memory: ").append(memoryMin).append(" pages\n");
        sb.append("  Globals: ").append(globalSection.size()).append("\n");
        sb.append("  Exports: ").append(exportSection.keySet()).append("\n");
        sb.append("  Code: ").append(codeSection.size()).append(" functions\n");
        sb.append("  Data: ").append(dataSection.size()).append(" segments\n");
        return sb.toString();
    }
}

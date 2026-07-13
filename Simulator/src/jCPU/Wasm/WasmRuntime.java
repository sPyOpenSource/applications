package jCPU.Wasm;

public class WasmRuntime {
    public final WasmStack stack = new WasmStack();
    public final WasmMemory memory;
    public final long[] globals;

    private final int[] callStack = new int[1024];
    private int callSp = 0;

    public int pc = 0;

    public WasmRuntime(int memoryMinPages, int memoryMaxPages, int globalCount) {
        this.memory = new WasmMemory(memoryMinPages, memoryMaxPages);
        this.globals = new long[globalCount];
    }

    public void pushCall(int returnAddr) {
        if (callSp >= callStack.length) {
            throw new RuntimeException("WASM call stack overflow");
        }
        callStack[callSp++] = returnAddr;
    }

    public int popCall() {
        if (callSp <= 0) {
            throw new RuntimeException("WASM call stack underflow");
        }
        return callStack[--callSp];
    }

    public boolean callStackEmpty() {
        return callSp == 0;
    }

    public void reset() {
        stack.clear();
        pc = 0;
        callSp = 0;
        for (int i = 0; i < globals.length; i++) {
            globals[i] = 0;
        }
    }
}

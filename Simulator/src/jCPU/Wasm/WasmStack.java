package jCPU.Wasm;

import java.util.ArrayList;
import java.util.List;

public class WasmStack {
    private final List<Long> values = new ArrayList<>();

    public void push(long value) {
        values.add(value);
    }

    public long pop() {
        if (values.isEmpty()) {
            throw new RuntimeException("WASM stack underflow");
        }
        return values.remove(values.size() - 1);
    }

    public long peek() {
        if (values.isEmpty()) {
            throw new RuntimeException("WASM stack underflow");
        }
        return values.get(values.size() - 1);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void clear() {
        values.clear();
    }

    public List<Long> getValues() {
        return new ArrayList<>(values);
    }
}

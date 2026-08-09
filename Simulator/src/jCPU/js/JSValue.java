package jCPU.js;

import java.util.LinkedHashMap;
import java.util.Map;

/** One run-time value in the JS subset (numbers, strings, bools, null, undefined, object). */
public abstract class JSValue {
    public static final int T_NUM = 1, T_STR = 2, T_BOOL = 3, T_NULL = 4, T_UNDEF = 5, T_OBJ = 6;
    public abstract int type();

    public static final class Num extends JSValue {
        public final double v;
        public Num(double v) { this.v = v; }
        @Override public int type() { return T_NUM; }
        @Override public String toString() { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }
    }
    public static final class Str extends JSValue {
        public final java.lang.String s;
        public Str(java.lang.String s) { this.s = s == null ? "" : s; }
        @Override public int type() { return T_STR; }
        @Override public String toString() { return s; }
    }
    public static final class Bool extends JSValue {
        public final boolean b;
        public Bool(boolean b) { this.b = b; }
        @Override public int type() { return T_BOOL; }
        @Override public String toString() { return String.valueOf(b); }
    }
    public static final JSValue NULL = new JSValue() {
        @Override public int type() { return T_NULL; }
        @Override public String toString() { return "null"; }
    };
    public static final JSValue UNDEFINED = new JSValue() {
        @Override public int type() { return T_UNDEF; }
        @Override public String toString() { return "undefined"; }
    };

    /** Plain object; used for {} and [] (nothing special). */
    public static final class Obj extends JSValue {
        public final Map<String, JSValue> props = new LinkedHashMap<>();
        @Override public int type() { return T_OBJ; }
        public JSValue get(String k) { return props.getOrDefault(k, UNDEFINED); }
        public void set(String k, JSValue v) { props.put(k, v); }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JSValue> e : props.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(e.getKey()).append(": ").append(e.getValue());
                first = false;
            }
            return sb.append("}").toString();
        }
    }
}
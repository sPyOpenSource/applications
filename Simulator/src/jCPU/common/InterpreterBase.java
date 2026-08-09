package jCPU.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared VM harness. Owns: run state, step counter, source line table,
 * and a stack of Frames (each = array of language statements + scope).
 * Concrete interpreters implement step()/loadSource()/visit() and use the
 * frame stack + pending fields for call/return.
 */
public abstract class InterpreterBase {
    public static final int STATE_IDLE = 0, STATE_RUNNING = 1,
                            STATE_ENDED = 2, STATE_ERROR = 3;

    /** One body: an Object[] of AST statements, the scope, and a "caller" link. */
    public static class Frame {
        public final Object code;
        public int pc;
        public final Object env;
        public final Frame parent;
        public Object pending;
        public Frame(Object code, Object env, Frame parent) {
            this.code = code; this.env = env; this.parent = parent;
        }
    }

    protected Frame frame;
    protected int state = STATE_IDLE;
    protected long steps = 0;
    protected List<String> sourceLines = new ArrayList<>();

    /** Parse+load a program; resets step state. Subclass hook. */
    public void loadSource(String src) {
        sourceLines.clear();
        if (src != null && !src.isEmpty())
            for (String s : src.split("\n", -1)) sourceLines.add(s.stripTrailing());
        reset();
    }

    public void reset() { frame = null; state = STATE_IDLE; steps = 0; }

    public int pc() { return frame == null ? -1 : frame.pc; }

    public String getDecodeAt(int pc) {
        if (pc < 0 || pc >= sourceLines.size()) return "";
        return pc + "\t" + sourceLines.get(pc);
    }

    public abstract int step() throws InterpreterException;

    /** Execute up to limit steps (limit<0 => until ended/error). */
    public void go(int limit) throws InterpreterException {
        while (state != STATE_ENDED && state != STATE_ERROR) {
            int p = step();
            if (limit > 0 && steps >= limit) break;
            if (p < 0) state = STATE_ENDED;
        }
    }

    protected Frame pushFrame(Object[] code, Object env) {
        Frame f = new Frame(code, env, frame);
        frame = f;
        return f;
    }
    protected void popFrameAndRestore() {
        if (frame != null) frame = frame.parent;
    }
}
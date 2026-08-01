# jx.verifier: the JVM's abstract-interpretation trust boundary

The user asked to learn about `jx.verifier` — the bytecode verifier of the armOS JVM (JX OS model). This is the first lesson on the JVM side of the codebase, as opposed to the four CPU simulators.

## What was learned

1. **Role**: The verifier is the security boundary. armOS prepares bytecode to run natively in supervisor context (no OS underneath), so type safety must be proven statically, before execution. `Verifier.java:28` is the orchestrator: build `ClassTree` → per-class checks → per-method analyses.

2. **Four analyses, one engine**: `TypeCheck` (types), `NullPointerAnalysis` (nullness), `FinalAndLeafAnalysis` (final/leaf flags for inlining), `WCETAnalysis` (worst-case execution time budget). All reuse `MethodVerifier`.

3. **The engine is abstract interpretation**: `MethodVerifier` parses a method into a linked CFG of `ByteCode` nodes (with targets/sources/exception handlers), then runs a **LIFO worklist** (`checkQueue`). Each node carries a `beforeState`; `JVMState.executeNextBC()` simulates one instruction and **merges** result states into successors; changed successors are re-queued until a fixed point is reached.

4. **The merge is the dataflow join**: stack depths must match (else VerifyException "Stacks of different size", `JVMOPStack.java:124`); object types merge to first common superclass or `T_OBJECT` (`TCObjectTypes.java:239`); `null` merges into any object type.

5. **The design insight**: `JVMState` is abstract, and each analysis supplies its own subclass + stack-element type (`TCState`/`TCTypes`, `NPAState`/`NPAValue`, `WCETState`/`WCETStackElement`). The verifier framework is a parameterized abstract-interpretation engine — a fourth analysis means subclassing `JVMState`, defining merge, registering in `Verifier`.

6. **Legacy wrinkle**: JSR/RET subroutines get a dedicated second mini-worklist (`SubroutineVerifier`) because return addresses make locals polymorphic; return addresses are abstract values (`TCRAType`). This marks it as a pre-Java-6 (pre-StackMapTable) verifier.

## Implications for next sessions

- Natural next step: go deep on **`BCStackEffect.simulateBC`** — the ~1,100-line table-driven simulation of all opcodes (the JVM-spec transfer functions) — to mirror the "opcode dispatch" lessons for the CPU simulators.
- Alternatively: how **WCET** builds its control-flow graph and bounds paths (the `wcet/` package is the most self-contained sub-analysis).
- Or: how the verified results (`NPAResult`, `FLAResult`) are consumed by the AOT/JIT compiler that turns bytecode into native armOS code.
- The mission's success criteria ("add a new instruction to any of the simulators and know exactly which files to touch") has a JVM analogue: adding a JVM opcode requires touching `ByteCode.java`, `BCStackEffect.java`, and the interpreter.

# Mission: Understand JavaScript & Python Engine Internals

## Goal
Understand how production-grade JavaScript engines (V8, SpiderMonkey, JavaScriptCore) and Python engines (CPython, PyPy) work internally — from parsing to execution to optimization.

## Why This Matters
- Debugging performance issues requires understanding engine internals
- Writing performant code benefits from knowing optimization triggers
- Security research requires understanding engine attack surfaces
- Language design decisions become clearer with implementation context

## Learning Path
1. **Foundations**: Interpreter architectures (tree-walking → bytecode → JIT)
2. **V8 Deep Dive**: Ignition interpreter, TurboFan compiler, hidden classes, inline caching
3. **SpiderMonkey**: Baseline interpreter, IonMonkey, type inference
4. **CPython Internals**: Bytecode VM, reference counting, GIL, memory allocator
5. **PyPy/JIT**: Meta-tracing JIT, RPython
6. **Advanced Topics**: GC strategies, inline caching, speculative optimization, WebAssembly

## Success Criteria
- Can explain the full pipeline from source to machine code in V8
- Can identify optimization opportunities from engine perspective
- Can read engine source code to debug issues
- Can articulate trade-offs between different engine designs
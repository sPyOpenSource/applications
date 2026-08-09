# JavaScript & Python Tree-Walking Interpreters — Design Spec

## Goal

Add JavaScript and Python virtual machines as selectable CPUs in the simulator framework. Each implements the `iCPU` interface with a tree-walking interpreter for a tiny teaching subset of the language. Programs are loaded as source code and executed step-by-step with source-level debugging via `getDecodeAt()`.

## Architecture

Two new `iCPU` implementations, independent of existing ARM/RISC-V/8051 CPUs:

```
┌─────────────────────────────────────────────────────────────┐
│                     Simulator Framework                      │
│  j51.conf  →  selects CPU class implementing iCPU           │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │   ARM       │ │  RISC-V     │ │  8051       │
       │ (existing)  │ │ (existing)  │ │ (existing)  │
       └─────────────┘ └─────────────┘ └─────────────┘
              ▲               ▲               ▲
              │               │               │
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │  JS VM      │ │  Python VM  │ │  (future)   │
       │  (NEW)      │ │  (NEW)      │ │             │
       └─────────────┘ └─────────────┘ └─────────────┘
```

- **`src/jCPU/js/JSInterpreter.java`** — implements `iCPU` for JavaScript
- **`src/jCPU/python/PythonInterpreter.java`** — implements `iCPU` for Python
- Each: Parser → AST → Tree-walking Interpreter
- Shared base: `InterpreterBase` (step/go, PC, call stack), `SourceLocator` (PC → source line)

## Components

### JSInterpreter (`src/jCPU/js/JSInterpreter.java`)

**Parser (recursive descent):**
- `parseProgram()` → `Program` (list of statements)
- Statements: `VarDecl`, `ExprStmt`, `IfStmt`, `WhileStmt`, `ForStmt`, `FunctionDecl`, `ReturnStmt`, `ThrowStmt`, `TryStmt`, `BlockStmt`
- Expressions: `BinaryExpr`, `UnaryExpr`, `CallExpr`, `MemberExpr`, `IndexExpr`, `ObjectLiteral`, `ArrayLiteral`, `Identifier`, `Literal`
- Precedence: assignment → logical → comparison → additive → multiplicative → unary → primary

**AST Nodes (package `jCPU.js.ast`):**
- Base: `Node`, `Statement`, `Expression`
- ~15 node classes, each with `accept(Visitor)` for interpreter

**Interpreter (tree-walking):**
- `Environment` — lexical scope chain (Map<String, Value> + parent Environment)
- `JSValue` — enum: `NUMBER`, `STRING`, `BOOLEAN`, `NULL`, `UNDEFINED`, `OBJECT`, `FUNCTION`
- `JSObject` — Map<String, JSValue> for properties; arrays as special objects with `length`
- `JSFunction` — holds `FunctionDecl` AST + closure `Environment`
- Call stack: `Frame` { function, environment, returnAddress, thisValue }
- `step()` executes one AST node, advances "PC" (node index in flattened AST)

**Built-ins:**
- `console.log(...)` → `System.out.println`
- `print(...)` alias

---

### PythonInterpreter (`src/jCPU/python/PythonInterpreter.java`)

**Parser (recursive descent, similar structure):**
- Statements: `Assign`, `ExprStmt`, `IfStmt`, `WhileStmt`, `ForStmt`, `FunctionDef`, `ReturnStmt`, `RaiseStmt`, `TryStmt`, `PassStmt`, `BreakStmt`, `ContinueStmt`
- Expressions: `BinOp`, `UnaryOp`, `Compare`, `Call`, `Attribute`, `Subscript`, `Dict`, `List`, `Tuple`, `Lambda`, `Name`, `Constant`
- Handles Python-specific: `elif`, `for...else`, `while...else`, multiple assignment, unpacking

**AST Nodes (package `jCPU.python.ast`):**
- ~18 node classes, visitor pattern

**Interpreter:**
- `PyEnvironment` — dict-based locals + globals + builtins
- `PyValue` — `INT`, `FLOAT`, `STR`, `BOOL`, `NONE`, `LIST`, `DICT`, `TUPLE`, `FUNCTION`
- `PyFunction` — holds `FunctionDef` AST + closure `Environment`
- Call stack similar to JS
- `step()` executes one AST node

**Built-ins:**
- `print(*args, **kwargs)` → `System.out.println`
- `len()`, `range()`, `str()`, `int()`, `float()`, `bool()`

---

### Shared Infrastructure

- `jCPU.common.InterpreterBase` — abstract base with `step()`, `go()`, PC management, call stack
- `jCPU.common.SourceLocator` — maps AST node index → (line, column) for `getDecodeAt()`
- `jCPU.common.Completion` — enum { NORMAL, RETURN, BREAK, CONTINUE, THROW } + value

## Language Subsets

### JavaScript (Tiny Teaching Subset)

| Feature | Included |
|---------|----------|
| `var`/`let`/`const`, numbers, strings, booleans | ✅ |
| `+ - * / %`, comparison, logical operators | ✅ |
| `if/else`, `while`, `for` loops | ✅ |
| Function declarations, calls, return, closures | ✅ |
| Object literals, property access, arrays | ✅ |
| `console.log` / `print` output | ✅ |
| `try/catch/finally`, `throw` | ✅ |
| Classes, modules, async/await, regex, Symbol, Map/Set | ❌ |

### Python (Tiny Teaching Subset)

| Feature | Included |
|---------|----------|
| `int`, `float`, `str`, `bool`, `None` | ✅ |
| `+ - * / // %`, comparison, logical operators | ✅ |
| `if/elif/else`, `while`, `for` loops | ✅ |
| `def`, calls, return, lambdas, closures | ✅ |
| `dict`, `list`, `tuple`, indexing/slicing | ✅ |
| `print()` output | ✅ |
| `try/except/finally`, `raise` | ✅ |
| Classes, generators, decorators, async, type hints, match/case | ❌ |

## Data Flow & Error Handling

**Execution Flow (`step()`):**
```
1. Fetch current AST node via PC (index into flattened statement list)
2. Dispatch to visit<NodeType>(node) → returns Completion { type, value }
3. Handle completion:
   - NORMAL: PC++
   - RETURN: pop call frame, set return value, PC = returnAddress
   - BREAK/CONTINUE: unwind to nearest loop/switch
   - THROW: unwind to nearest try/catch (JS) or try/except (Python)
4. Return instruction text for GUI (from SourceLocator)
```

**Memory Model:**
- No raw memory access — high-level language VMs
- `iCPU` memory methods (`getDirect`, `setDirect`, `idata`, `xdata`) return 0 / no-op
- `getDecodeAt(pc)` returns source line at current AST node
- `pc()` returns current AST node index

**Error Handling:**
- **Parse errors:** Thrown during `reset()` / program load — `ParseException` with line/column
- **Runtime errors:** `RuntimeException` subclasses:
  - `ReferenceError` (JS) / `NameError` (Python) — undefined variable
  - `TypeError` — invalid operation on type
  - `RangeError` — array index out of bounds
- All caught in `step()`, wrapped in `InterpreterException` with PC + stack trace
- `go(limit)` stops on exception, returns error via `getDecodeAt()` showing error line

**Resource Limits (configurable):**
- Max call stack depth (default 1000)
- Max steps per `go()` (enforced by caller)
- Max recursion depth for `toString()` / `repr()`

## Testing Plan

**Unit Tests (per VM):**
- `test/jCPU/js/JSLexerTest.java` — tokenizes all language constructs
- `test/jCPU/js/JSParserTest.java` — parses valid programs, rejects invalid
- `test/jCPU/js/JSInterpreterTest.java` — executes ASTs, asserts final environment state
  - Arithmetic, variables, control flow, functions, objects/arrays, exceptions

- `test/jCPU/python/PythonLexerTest.java`
- `test/jCPU/python/PythonParserTest.java`
- `test/jCPU/python/PythonInterpreterTest.java`
  - Mirrors JS tests with Python syntax
  - Collections, `for...else`, multiple assignment

**Integration Tests:**
- `test/jCPU/js/JSProgramTest.java` — loads `.js` files from `test/programs/js/`, runs via `go()`, asserts output
- `test/jCPU/python/PythonProgramTest.java` — same for `.py` files
- Test programs: `fibonacci`, `factorial`, `closures`, `objects`/`classes`

**GUI/Disassembly Tests:**
- `test/jCPU/js/JSDecodeTest.java` — `getDecodeAt(pc)` returns correct source line at each step
- `test/jCPU/python/PythonDecodeTest.java`

**Framework Integration:**
- `test/jCPU/JSInterpreterIntegrationTest.java` — loads via `j51.conf`, runs `step()`/`go()`, verifies `iCPU` contract
- `test/jCPU/PythonInterpreterIntegrationTest.java`

## File Changes

### New Files
| File | Purpose |
|------|---------|
| `src/jCPU/js/JSInterpreter.java` | Main iCPU implementation for JS |
| `src/jCPU/js/ast/*.java` | ~15 AST node classes |
| `src/jCPU/js/JSParser.java` | Recursive descent parser |
| `src/jCPU/js/JSLexer.java` | Tokenizer |
| `src/jCPU/js/JSValue.java` | Value representation |
| `src/jCPU/js/Environment.java` | Lexical scope |
| `src/jCPU/python/PythonInterpreter.java` | Main iCPU implementation for Python |
| `src/jCPU/python/ast/*.java` | ~18 AST node classes |
| `src/jCPU/python/PythonParser.java` | Recursive descent parser |
| `src/jCPU/python/PythonLexer.java` | Tokenizer |
| `src/jCPU/python/PyValue.java` | Value representation |
| `src/jCPU/python/PyEnvironment.java` | Scope (locals/globals/builtins) |
| `src/jCPU/common/InterpreterBase.java` | Shared step/go/PC/call stack |
| `src/jCPU/common/SourceLocator.java` | AST node → source location |
| `src/jCPU/common/Completion.java` | Completion type enum |
| `test/jCPU/js/*.java` | Unit + integration tests |
| `test/jCPU/python/*.java` | Unit + integration tests |
| `test/programs/js/*.js` | Sample programs |
| `test/programs/python/*.py` | Sample programs |

### Modified Files
| File | Change |
|------|--------|
| `j51.conf` | Add lines for `jCPU.js.JSInterpreter` and `jCPU.python.PythonInterpreter` |

### Unchanged
All ARM, RISC-V, 8051, GUI, peripheral, and existing test files.

## Success Criteria

1. Both VMs appear in `j51.conf` and can be selected as the active CPU
2. `step()` executes one AST node, `go(N)` runs up to N steps
3. `getDecodeAt(pc)` returns the source line corresponding to the current AST node
4. All unit tests pass for lexer, parser, interpreter
5. Integration test programs (fibonacci, factorial, closures, objects) produce correct output
6. Parse errors show line/column; runtime errors show stack trace with source locations
7. No changes required to existing ARM/RISC-V/8051 code
8. Both VMs implement the full `iCPU` interface (all default methods no-op where not applicable)
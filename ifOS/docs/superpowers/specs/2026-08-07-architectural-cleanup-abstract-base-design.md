# Architectural Cleanup: Abstract Base Refactor (Revised)

**Date:** 2026-08-07 (revised after codebase audit)
**Project:** ifOS (JX OS API/Specification Layer)
**Status:** Approved for Implementation

---

## 1. Corrected Context

A codebase audit corrected two assumptions of the original design:

1. **ifOS is an API/specification module, not a self-contained kernel module.**
   - The three target subsystems (`src/jx/devices`, `src/jx/fs`, `src/jx/zero/verifier`) contain **77 interface files and zero concrete classes**.
   - **220 files** across 7 sibling projects (`APP`, `FS`, `GUI`, `HCI`, `NET`, `Simulator`, `WM`) import `jx.devices`, `jx.fs`, or `jx.zero.verifier`.
   - **~45 files** in those projects directly `implements` the interfaces in scope (e.g. `NET/src/jx/net/devices/lance/Lance.java` → `NetworkDevice`, `Simulator/src/jx/verifier/MethodVerifier.java` → `VerifierInterface`, `WM/src/jx/keyboard/KeyboardImpl.java` → `Keyboard`, `FS/src/bioide/Drive.java` → `BlockIO`).
2. **Deleting interfaces is not possible in this module alone.** Removing `Bus`, `PCIDevice`, `FileSystemInterface`, etc. breaks compilation of the sibling projects. The "~11 concrete classes" of the original plan do not exist in-tree; they live in sibling projects.

**Build:** the module compiles standalone via its existing ant build into `dist/ifOS.jar`. It has no test suite.

## 2. Problem Statement (unchanged)

The `ifOS` codebase relies on a massive hierarchy of interfaces (100+ in `src/jx` alone) with very few in-tree implementations. This "Interface Forest" creates:

- **High Cognitive Load:** New implementers must satisfy 3-4 interface layers (`Device` → `Bus` → `PCIDevice` → `Portal`) to implement one device.
- **Rigidity:** Java's single-inheritance-for-classes vs multiple-inheritance-for-interfaces forces awkward `extends Portal`/`extends Bus` chains that don't model the domain.
- **No Composition:** Cross-cutting concerns (block I/O, input, display) are modeled as "is-a" inheritance instead of "has-a" capabilities.

## 3. Design Goals (revised)

1.  **Flatten the implementer contract:** New device/fs/verifier implementations target **one abstract base + capability interfaces**, not a 4-deep `implements` chain. New code reaches depth ≤ 2 (`AbstractDevice` → concrete class).
2.  **Capability composition:** Replace "Is-A" inheritance with "Has-A" capability interfaces for orthogonal concerns (PCI, BlockIO, Input, Display).
3.  **Backward compatibility:** All 220 consumer files in sibling projects keep compiling **unchanged**. No interface is deleted and no existing public signature changes.
4.  **Deprecate, don't delete:** Legacy hierarchy interfaces are marked `@Deprecated` with migration pointers, signaling the new pattern without breaking anyone.
5.  **Precondition for future deletion:** Once sibling implementations migrate (a separate, later cross-project plan), the deprecated interfaces can be deleted. That deletion is **explicitly out of scope** for this plan.

## 4. Solution: Abstract Base Architecture (additive, backward-compatible)

### 4.1 Core Principles

1.  **One Abstract Base per Subsystem:** Each major domain gets exactly one abstract class that new implementations extend.
2.  **Abstract bases implement their legacy interfaces:** e.g. `AbstractDevice implements Device` — so a migrated device still passes `instanceof Device` and can be handed to consumers typed against the old API.
3.  **Interfaces for Capabilities Only:** New interfaces exist only for mixin-style capabilities (`PciCapable`, `BlockIOCapable`, `InputCapable`, `DisplayCapable`) or standard contracts (`AutoCloseable`).
4.  **Protected State in Base:** Shared fields (`deviceId`, `config`, verifier state) live in the abstract base as `protected`.
5.  **Template Lifecycle in Base:** Shared lifecycle (`open` → `init`, `close`) implemented in the base; subclasses override narrow hooks.
6.  **Bridge code suppresses deprecation warnings:** New classes that intentionally implement deprecated legacy interfaces carry `@SuppressWarnings("deprecation")` with a comment.

### 4.2 New API Surface (10 files)

All new files are additive. Nothing existing is modified except the deprecation annotations in 4.4.

**Capability Interfaces** (`src/jx/devices/`):

```java
public interface PciCapable      { PCIAccess getPciAccess(); }
public interface BlockIOCapable  { BlockIO   getBlockIO(); }
public interface InputCapable    { Keyboard  getKeyboard(); Mouse getMouse(); }
public interface DisplayCapable  { Screen    getScreen(); }
```

**Abstract Bases:**

| File | Extends / Implements | Purpose |
|------|----------------------|---------|
| `src/jx/devices/AbstractDevice.java` | `implements Device, AutoCloseable` | Base for all devices. `protected final int deviceId`, `protected DeviceConfiguration config`; template `open()` → `validateConfig()` + `init()`; default `close()`; `getId()`; abstract `getSupportedConfigurations()`. |
| `src/jx/devices/net/AbstractNetworkDevice.java` | `extends AbstractDevice implements NetworkDevice` | Base for NICs. Hosts `RECEIVE_MODE_*` constants; inherits device lifecycle. |
| `src/jx/fs/AbstractFileSystem.java` | `implements FileSystemInterface, AutoCloseable` | Base for filesystems. Default `close()` delegates to `unmount()`; rest of `FileSystemInterface` abstract. |
| `src/jx/zero/verifier/AbstractVerifier.java` | `implements VerifierInterface` | Base for bytecode verifiers. Holds `protected MethodSource method`, `protected Subroutines srs`, `protected Object parameter`; template `runChecks()` → `checkBC()` loop → `endChecks()`. |
| `src/jx/zero/verifier/LocalVarsStrategy.java` | interface | Replaces `NPALocalVarsInterface`: `void write(int index, NPAValue type, int bcAddr); NPAValue read(int index); void setValue(NPAValue value, int newVal);` |
| `src/jx/zero/verifier/TypeCheckStrategy.java` | interface | Replaces `TCLocalVarsInterface`: `void write(int index, TCTypes type, int bcAddr); TCTypes read(int index, TCTypes type);` |

All abstract bases that implement a deprecated legacy interface carry `@SuppressWarnings("deprecation")`.

### 4.3 Target Implementation Patterns

**Before (implementer burden):**
```java
public class MyPCIDevice implements Device, Bus, PCIDevice, Portal {
    public DeviceConfigurationTemplate[] getSupportedConfigurations() { ... }
    public void open(DeviceConfiguration conf) { ... }
    public void close() { ... }
    public int getId() { ... }
    public Device getChild(int index) { ... }
    // ... 20+ PCIDevice methods
}
```

**After:**
```java
public class MyPCIDevice extends AbstractDevice implements PciCapable, BlockIOCapable {
    public MyPCIDevice() { super(0); }
    protected void init(DeviceConfiguration conf) { /* real setup */ }
    public DeviceConfigurationTemplate[] getSupportedConfigurations() { ... }
    public PCIAccess getPciAccess() { ... }
    public BlockIO getBlockIO() { ... }
}
```

### 4.4 Deprecation of Legacy Interfaces

Exactly these 9 hierarchy interfaces get `@Deprecated` + a `@deprecated` javadoc tag naming the replacement:

| Legacy interface | Replace with |
|------------------|--------------|
| `jx.devices.Device` | `AbstractDevice` |
| `jx.devices.Bus` | `AbstractDevice` + capability interfaces |
| `jx.devices.ide.IDEDevice` | `AbstractDevice` + `PciCapable` / `BlockIOCapable` |
| `jx.devices.net.NetworkDevice` | `AbstractNetworkDevice` |
| `jx.devices.pci.PCIDevice` | `AbstractDevice` + `PciCapable` |
| `jx.fs.FileSystemInterface` | `AbstractFileSystem` |
| `jx.zero.verifier.VerifierInterface` | `AbstractVerifier` |
| `jx.zero.verifier.npa.NPALocalVarsInterface` | `LocalVarsStrategy` |
| `jx.zero.verifier.typecheck.TCLocalVarsInterface` | `TypeCheckStrategy` |

The following leaf types remain **canonical value types and are NOT deprecated** (they are the return types of the capability interfaces). They get a javadoc paragraph describing the new composition pattern instead:
`jx.devices.bio.BlockIO`, `jx.devices.Keyboard`, `jx.devices.Mouse`, `jx.devices.Screen`.

**Explicitly out of scope (untouched):** `jx.fs.db.*`, `jx.fs.buffer.*`, `jx.fs.{Node,FS,FileSystem,StatFS,FSAttribute,Permission,VolumeManager}`, `jx.devices.{DeviceFinder,KeyListener,DeviceConfiguration,DeviceConfigurationTemplate}`, `jx.devices.pci.{PCI,PCIAccess,PCIAddress,PCICap}`, `jx.devices.framebuffer.*`.

## 5. Migration Plan

| Phase | Scope | Breaking? | Deliverable |
|-------|-------|-----------|-------------|
| 1. Capabilities | Add `PciCapable`, `BlockIOCapable`, `InputCapable`, `DisplayCapable` | No | 4 new interfaces; build green |
| 2. Abstract Bases | Add `AbstractDevice`, `AbstractNetworkDevice`, `AbstractFileSystem`, `AbstractVerifier` + 2 strategy interfaces | No | 6 new classes; build green |
| 3. Deprecate | Add `@Deprecated` + `@deprecated` javadoc to 9 legacy interfaces; composition notes to 4 leaf interfaces | No | Build green (deprecation warnings expected) |
| 4. Document | Migration guide with full code examples | No | `docs/superpowers/` guide |
| 5. Future work | Migrate ~45 sibling implementations; delete deprecated interfaces; investigate `Node`/`FS` consolidation | Yes | **Not in this plan** |

## 6. Risk Assessment (updated)

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking 220 sibling consumers | Low | High | Additive-only scope; no signature changes; explicit "no sibling source change" rule; build gate. |
| Deprecation-warning noise in sibling builds | Medium | Low | Warnings are the intended migration signal; only new bridge code suppresses them (`@SuppressWarnings`). |
| Abstract base becomes "God Class" | Medium | Medium | Rule: base holds only *shared* state + lifecycle template; orthogonal concerns live in capability interfaces. |
| javac rejects implementing a deprecated interface | Low | High | javac allows it (warning only); verified by the build gate in every phase. |

## 7. Success Criteria

- [ ] `ant` build produces `dist/ifOS.jar` with **zero errors**.
- [ ] Exactly 10 new API files added; no existing public signature changed (annotations/doc only).
- [ ] All 9 listed legacy interfaces carry `@Deprecated` + `@deprecated` javadoc with the replacement pointer (**100% coverage of the listed set**).
- [ ] `BlockIO`, `Keyboard`, `Mouse`, `Screen` carry the composition-pattern javadoc note.
- [ ] Abstract bases satisfy their legacy interfaces (`new MyPCIDevice() instanceof Device` holds).
- [ ] Migration guide contains ≥ 1 complete, compilable-style code example of `extends AbstractDevice` + capabilities.
- [ ] Zero changes to any file outside `src/jx` (verified by `git status` of the enclosing `test/` repo).

---

*End of Specification*

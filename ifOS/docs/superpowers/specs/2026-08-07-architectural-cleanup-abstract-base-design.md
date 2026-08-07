# Architectural Cleanup: Abstract Base Refactor

**Date:** 2026-08-07
**Project:** ifOS (Java Microkernel OS)
**Status:** Approved for Implementation

---

## 1. Problem Statement

The `ifOS` codebase currently relies on a massive hierarchy of interfaces (100+ in `src/jx` alone) with very few concrete implementations (~11 `public class` files). This "Interface Forest" creates:

- **High Cognitive Load:** Developers must navigate 3-4 interface layers (`PCIDevice` → `Bus` → `Device` → `Portal`) to understand a single type.
- **Zero Reuse:** Most interfaces have exactly zero or one implementations, making the abstraction purely theoretical.
- **Maintenance Burden:** Adding cross-cutting concerns (logging, metrics, validation) requires touching multiple interface files or creating yet another interface layer.
- **Rigidity:** Java's single-inheritance-for-classes vs multiple-inheritance-for-interfaces forces awkward `extends Portal` chains that don't model the domain well.

## 2. Design Goals

1.  **Reduce File Count:** Target 60-70% reduction in `.java` files in `src/jx/devices`, `src/jx/fs`, `src/jx/zero/verifier`.
2.  **Shallow Hierarchy:** Max depth of 2 (Abstract Base → Concrete Class).
3.  **Single Source of Truth:** Shared logic lives in one abstract class, not scattered across default methods in interfaces.
4.  **Capability Composition:** Replace "Is-A" inheritance with "Has-A" capability interfaces for orthogonal concerns (BlockIO, Display, Input).

## 3. Solution: Abstract Base Architecture

### 3.1 Core Principles

1.  **One Abstract Base per Subsystem:** Each major domain gets exactly one abstract class.
2.  **Interfaces for Capabilities Only:** Interfaces are retained only for mixin-style capabilities (e.g., `BlockIOCapable`) or standard Java contracts (`AutoCloseable`).
3.  **Protected State in Base:** Shared fields (ID, config, logger) live in the abstract base as `protected`.
4.  **Default Behavior in Base:** Common boilerplate (validation, resource tracking) implemented as `protected` helpers in the base class.

### 3.2 Target Hierarchies

#### A. Device Subsystem (`jx.devices`)

| Current | New |
|---------|-----|
| `Device` → `Bus` → `PCIDevice` / `IDEDevice` | `abstract class AbstractDevice` implements `AutoCloseable`<br>`PCIDevice extends AbstractDevice`<br>`IDEDevice extends AbstractDevice` |
| `NetworkDevice` (extends `Device`, `Portal`) | `abstract class AbstractNetworkDevice extends AbstractDevice` |
| `BlockIO`, `Keyboard`, `Mouse`, `Screen` (extend `Portal`) | **Capability Interfaces:**<br>`interface BlockIOCapable { BlockIO getBlockIO(); }`<br>`interface InputCapable { Keyboard getKeyboard(); Mouse getMouse(); }`<br>`interface DisplayCapable { Screen getScreen(); }` |
| `DeviceFinder` | Keep as Interface (Factory pattern) |
| `DeviceConfiguration` / `Template` | Keep as Records/Classes (Data) |

**Key Change:** `Bus` is removed as a type. PCI/IDE configuration becomes a capability of a Device.

#### B. Filesystem Subsystem (`jx.fs`)

| Current | New |
|---------|-----|
| `FileSystemInterface` (extends `Portal`) | `abstract class AbstractFileSystem` implements `AutoCloseable` |
| `FSObject`, `Directory`, `File`, `Node` | `abstract class AbstractFSObject` → `AbstractDirectory`, `AbstractFile` |
| `BufferCache`, `BufferHead`, `Buffer` | Package-private implementation classes |

#### C. Verifier Subsystem (`jx.zero.verifier`)

| Current | New |
|---------|-----|
| `VerifierInterface`, `NPALocalVarsInterface`, `TCLocalVarsInterface` | `abstract class AbstractVerifier` + Strategy Interfaces:<br>`interface LocalVarsStrategy { ... }`<br>`interface TypeCheckStrategy { ... }` |

### 3.3 Capability Interface Pattern

Instead of:
```java
public interface PCIDevice extends Bus { ... }
public class MyPCIDevice implements PCIDevice { ... }
```

We use:
```java
public abstract class AbstractDevice implements AutoCloseable {
    protected final int deviceId;
    protected DeviceConfiguration config;
    // Shared logic: open(), close(), validateConfig(), getLogger()
}

public interface PciCapable {
    PCIAccess getPciAccess();
    PCIDevice getPciDevice();
}

public class MyPCIDevice extends AbstractDevice implements PciCapable {
    // Implements getPciAccess(), inherits open()/close()
}
```

**Benefits:**
- A device can be `PciCapable` AND `BlockIOCapable` without inheritance conflicts.
- New capabilities added without touching the base hierarchy.
- Clear separation: "What it IS" (AbstractDevice) vs "What it CAN DO" (Capabilities).

## 4. Migration Plan

### Phase 1: Create Abstract Bases (Non-Breaking)
1. Add `AbstractDevice`, `AbstractFileSystem`, `AbstractVerifier` alongside existing interfaces.
2. Populate with `protected` constructors, shared fields, and `protected` helper methods.
3. Add Capability Interfaces (`PciCapable`, `BlockIOCapable`, etc.).

### Phase 2: Refactor Implementations (Breaking)
1. Update the ~11 concrete classes to `extends AbstractDevice` (or respective base).
2. Implement required Capability Interfaces.
3. Remove `implements Bus`, `implements PCIDevice`, etc.

### Phase 3: Cleanup
1. Delete unused interface files (`Bus.java`, `PCIDevice.java` (interface), `BlockIO.java` (interface), `Portal.java` if unused).
2. Update method signatures in `DeviceFinder`, `VolumeManager`, etc., to accept Abstract Bases or Capability Interfaces.
3. Run full build/test cycle.

## 5. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking external consumers | Low | High | This is an internal kernel module; no external API consumers identified. |
| Missing an implementation | Low | Medium | Only ~11 concrete classes exist; manual audit is trivial. |
| Abstract Base becomes "God Class" | Medium | Medium | Strict rule: Base only holds *shared* logic. Capabilities stay in interfaces. |

## 6. Success Criteria

- [ ] File count in `src/jx` reduced by ≥ 60%.
- [ ] Maximum inheritance depth = 2.
- [ ] Zero interfaces with only one implementation.
- [ ] All existing tests (if any) pass.
- [ ] New device can be added by extending one class + implementing 1-2 capabilities.

---

*End of Specification*
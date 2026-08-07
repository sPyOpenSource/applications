# Architectural Cleanup: Abstract Base Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backward-compatible abstract-base + capability-interface API to the `ifOS` module so new device/filesystem/verifier implementations target one class instead of a 4-deep interface chain, and deprecate the legacy hierarchy interfaces without breaking any consumer.

**Architecture:** Purely additive. Four abstract base classes (`AbstractDevice`, `AbstractNetworkDevice`, `AbstractFileSystem`, `AbstractVerifier`) each implement their legacy interface for backward compatibility. Four capability interfaces (`PciCapable`, `BlockIOCapable`, `InputCapable`, `DisplayCapable`) model "has-a" concerns. Two strategy interfaces (`LocalVarsStrategy`, `TypeCheckStrategy`) replace the verifier's local-vars interfaces. Nine legacy hierarchy interfaces get `@Deprecated` + migration javadoc; nothing is deleted.

**Tech Stack:** Java (javac via Apache Ant, NetBeans project layout), `ant` build producing `dist/ifOS.jar`. No test framework exists in this module; the verification gate is the ant compile/jar build.

## Global Constraints

- **Working root:** `/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS` (repo root of the `testOS` git submodule).
- **Additive-only:** Create 10 new API files; the only edits to existing files are adding `@Deprecated`, javadoc tags, and javadoc paragraphs. No existing public signature may change.
- **No changes outside `src/jx` and `docs/superpowers/`.** Do not touch `src/java/`, sibling projects (`../APP`, `../FS`, `../GUI`, `../HCI`, `../NET`, `../Simulator`, `../WM`), or any `dist/*.jar`.
- **Bridge rule:** any new class that implements a deprecated legacy interface must carry `@SuppressWarnings("deprecation")`.
- **Deprecation set (exactly 9):** `jx.devices.Device`, `jx.devices.Bus`, `jx.devices.ide.IDEDevice`, `jx.devices.net.NetworkDevice`, `jx.devices.pci.PCIDevice`, `jx.fs.FileSystemInterface`, `jx.zero.verifier.VerifierInterface`, `jx.zero.verifier.npa.NPALocalVarsInterface`, `jx.zero.verifier.typecheck.TCLocalVarsInterface`.
- **Leaf types are NOT deprecated:** `jx.devices.bio.BlockIO`, `jx.devices.Keyboard`, `jx.devices.Mouse`, `jx.devices.Screen` get a javadoc note only.
- **Verification commands (run from the working root):** `ant compile` and `ant jar`. Both must end `BUILD SUCCESSFUL`. **Never run plain `ant`** — the `javadoc` target fails on pre-existing errors in `src/java/**` (copied JDK sources), unrelated to this plan.
- **Staging discipline:** the enclosing `test/` repo has unrelated pre-existing modifications (sibling projects, dist jars). Stage ONLY the files each task names — never `git add -A` or `git add .`. Confirm with `git status` before committing.
- **Deprecation warnings are expected** during `ant compile` after Task 7 (e.g. `DeviceFinder.find()` returns the deprecated `Device`). `BUILD SUCCESSFUL` with warnings is a pass; the build does not use `-Werror`.

---

### Task 1: Capability Interfaces

**Files:**
- Create: `src/jx/devices/PciCapable.java`
- Create: `src/jx/devices/BlockIOCapable.java`
- Create: `src/jx/devices/InputCapable.java`
- Create: `src/jx/devices/DisplayCapable.java`

**Interfaces:**
- Consumes: `jx.devices.pci.PCIAccess`, `jx.devices.bio.BlockIO`, `jx.devices.Keyboard`, `jx.devices.Mouse`, `jx.devices.Screen` (all already exist in the module).
- Produces: `PciCapable.getPciAccess() : PCIAccess`, `BlockIOCapable.getBlockIO() : BlockIO`, `InputCapable.getKeyboard() : Keyboard`, `InputCapable.getMouse() : Mouse`, `DisplayCapable.getScreen() : Screen`.

- [ ] **Step 1: Create `PciCapable.java`**

```java
package jx.devices;

import jx.devices.pci.PCIAccess;

/**
 * Capability interface: this device provides access to the PCI bus.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code jx.devices.pci.PCIDevice}.
 */
public interface PciCapable {
    PCIAccess getPciAccess();
}
```

- [ ] **Step 2: Create `BlockIOCapable.java`**

```java
package jx.devices;

import jx.devices.bio.BlockIO;

/**
 * Capability interface: this device provides block I/O.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code jx.devices.bio.BlockIO} directly.
 */
public interface BlockIOCapable {
    BlockIO getBlockIO();
}
```

- [ ] **Step 3: Create `InputCapable.java`**

```java
package jx.devices;

/**
 * Capability interface: this device provides a keyboard and/or mouse.
 * A device that does not provide one of the two returns {@code null}.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code Keyboard} / {@code Mouse} directly.
 */
public interface InputCapable {
    Keyboard getKeyboard();
    Mouse getMouse();
}
```

- [ ] **Step 4: Create `DisplayCapable.java`**

```java
package jx.devices;

/**
 * Capability interface: this device provides a display.
 *
 * <p>Compose with {@link AbstractDevice} instead of implementing
 * {@code Screen} directly.
 */
public interface DisplayCapable {
    Screen getScreen();
}
```

- [ ] **Step 5: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL` (no new warnings).

- [ ] **Step 6: Commit**

```bash
git add src/jx/devices/PciCapable.java src/jx/devices/BlockIOCapable.java src/jx/devices/InputCapable.java src/jx/devices/DisplayCapable.java
git commit -m "feat(devices): add capability interfaces (PciCapable, BlockIOCapable, InputCapable, DisplayCapable)"
```

---

### Task 2: `AbstractDevice` Base Class

**Files:**
- Create: `src/jx/devices/AbstractDevice.java`

**Interfaces:**
- Consumes: `Device` (legacy), `AutoCloseable` (JDK), `DeviceConfiguration`, `DeviceConfigurationTemplate` (all in `jx.devices`).
- Produces: `protected AbstractDevice(int deviceId)`, `protected AbstractDevice(DeviceConfiguration config)` (leaves `deviceId` at 0), `public int getId()`, `public void open(DeviceConfiguration conf)` (call exactly once), `public void close()`, `protected void validateConfig(DeviceConfiguration conf)`, `protected void deinit()`, `protected abstract void init(DeviceConfiguration conf)`, `public abstract DeviceConfigurationTemplate[] getSupportedConfigurations()`. Fields: `protected final int deviceId`, `protected DeviceConfiguration config`. A rationale comment sits between the class javadoc and the `@SuppressWarnings("deprecation")`.

- [ ] **Step 1: Create `AbstractDevice.java`**

```java
package jx.devices;

/**
 * Base class for all devices.
 *
 * <p>Lifecycle is driven by {@code open()} (validate → init) and
 * {@code close()} (deinit). Subclasses override {@link #init(DeviceConfiguration)}
 * and {@link #deinit()} for real setup/teardown and
 * {@link #validateConfig(DeviceConfiguration)} for config validation.
 *
 * <p>Implements the legacy {@code Device} interface so that instances still
 * satisfy {@code instanceof Device} for consumers of the old API.
 */
// implements deprecated Device so instances satisfy instanceof Device for legacy consumers
@SuppressWarnings("deprecation")
public abstract class AbstractDevice implements Device, AutoCloseable {
    protected final int deviceId;
    protected DeviceConfiguration config;

    /**
     * Creates a device with the given device id.
     *
     * <p>The {@link #AbstractDevice(DeviceConfiguration)} constructor leaves
     * {@code deviceId} at {@code 0}.
     */
    protected AbstractDevice(int deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Creates a device from a config without an explicit device id.
     *
     * <p>This constructor leaves {@code deviceId} at {@code 0}, so
     * {@link #getId()} returns {@code 0} until a real id is available.
     */
    protected AbstractDevice(DeviceConfiguration config) {
        this.deviceId = 0;
        this.config = config;
    }

    /**
     * Returns the device id ({@code 0} when created from config only).
     */
    public int getId() {
        return deviceId;
    }

    /**
     * Opens the device with the given configuration.
     *
     * <p>Must be called exactly once; the base does not guard against double
     * invocation. Overriding it without calling {@code super.open(...)}
     * bypasses validation, for advanced subclasses.
     */
    public void open(DeviceConfiguration conf) {
        validateConfig(conf);
        this.config = conf;
        init(conf);
    }

    /**
     * Closes the device, releasing its configuration.
     */
    public void close() {
        deinit();
        this.config = null;
    }

    /**
     * Validates a configuration before the device is opened.
     *
     * <p>Runs BEFORE config is stored, so it cannot inspect the previously
     * held config.
     */
    protected void validateConfig(DeviceConfiguration conf) {
    }

    /**
     * Performs the real device setup for the given configuration.
     */
    protected abstract void init(DeviceConfiguration conf);

    /**
     * Performs device teardown; called by {@link #close()} before the config
     * is released. Intended as a no-op hook for subclasses to override.
     */
    protected void deinit() {
    }

    /**
     * Returns the configurations this device supports.
     */
    public abstract DeviceConfigurationTemplate[] getSupportedConfigurations();
}
```

- [ ] **Step 2: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL` (the `@SuppressWarnings("deprecation")` prevents warnings from implementing deprecated `Device`).

- [ ] **Step 3: Commit**

```bash
git add src/jx/devices/AbstractDevice.java
git commit -m "feat(devices): add AbstractDevice base class"
```

---

### Task 3: `AbstractNetworkDevice` Base Class

**Files:**
- Create: `src/jx/devices/net/AbstractNetworkDevice.java`

**Interfaces:**
- Consumes: `AbstractDevice` (Task 2), `DeviceConfiguration`, legacy `NetworkDevice` (its `extends Device, Portal` chain is satisfied by the inheritance from `AbstractDevice`).
- Produces: `AbstractNetworkDevice(int deviceId)` and `AbstractNetworkDevice(DeviceConfiguration config)` protected constructors; `public static final int RECEIVE_MODE_INDIVIDUAL = 1`, `RECEIVE_MODE_PROMISCOUS = 2`, `RECEIVE_MODE_MULTICAST = 3`. Network device behavior (`setReceiveMode`, `transmit`, `getMACAddress`, `getMTU`, `registerNonBlockingConsumer`) stays abstract for subclasses.

- [ ] **Step 1: Create `AbstractNetworkDevice.java`**

```java
package jx.devices.net;

import jx.devices.AbstractDevice;
import jx.devices.DeviceConfiguration;

/**
 * Base class for network interface controllers.
 *
 * <p>Extends {@link AbstractDevice} and implements the legacy
 * {@code NetworkDevice} interface, so NIC implementations only need to
 * supply the network behavior methods and their device configuration.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractNetworkDevice extends AbstractDevice implements NetworkDevice {

    public static final int RECEIVE_MODE_INDIVIDUAL = 1;
    public static final int RECEIVE_MODE_PROMISCOUS = 2;
    public static final int RECEIVE_MODE_MULTICAST  = 3;

    protected AbstractNetworkDevice(int deviceId) {
        super(deviceId);
    }

    protected AbstractNetworkDevice(DeviceConfiguration config) {
        super(config);
    }
}
```

- [ ] **Step 2: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/jx/devices/net/AbstractNetworkDevice.java
git commit -m "feat(devices): add AbstractNetworkDevice base class"
```

---

### Task 4: `AbstractFileSystem` Base Class

**Files:**
- Create: `src/jx/fs/AbstractFileSystem.java`

**Interfaces:**
- Consumes: legacy `FileSystemInterface` (extends `jx.zero.Portal`), `AutoCloseable` (JDK), `Permission`.
- Produces: `protected AbstractFileSystem(String name)`, `public String getName()`, `public void close() throws Exception`. Remaining `FileSystemInterface` methods (`getDefaultPermission`, `openRootDirectoryRO`, `openRootDirectoryRW`, `mount`, `unmount`) stay abstract.

- [ ] **Step 1: Create `AbstractFileSystem.java`**

```java
package jx.fs;

/**
 * Base class for filesystem implementations.
 *
 * <p>Subclasses supply {@code mount()} / {@code unmount()} and the root
 * directory accessors. {@link #close()} delegates to {@link #unmount()}.
 *
 * <p>Implements the legacy {@code FileSystemInterface} so instances still
 * satisfy the old API for consumers.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractFileSystem implements FileSystemInterface, AutoCloseable {
    protected final String name;

    protected AbstractFileSystem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void close() throws Exception {
        unmount();
    }
}
```

- [ ] **Step 2: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/jx/fs/AbstractFileSystem.java
git commit -m "feat(fs): add AbstractFileSystem base class"
```

---

### Task 5: Verifier Strategy Interfaces

**Files:**
- Create: `src/jx/zero/verifier/LocalVarsStrategy.java`
- Create: `src/jx/zero/verifier/TypeCheckStrategy.java`

**Interfaces:**
- Consumes: `jx.zero.verifier.npa.NPAValue`, `jx.zero.verifier.typecheck.TCTypes`.
- Produces: `LocalVarsStrategy.write(int index, NPAValue type, int bcAddr)`, `LocalVarsStrategy.read(int index) : NPAValue`, `LocalVarsStrategy.setValue(NPAValue value, int newVal)`; `TypeCheckStrategy.write(int index, TCTypes type, int bcAddr)`, `TypeCheckStrategy.read(int index, TCTypes type) : TCTypes`.

- [ ] **Step 1: Create `LocalVarsStrategy.java`**

```java
package jx.zero.verifier;

import jx.zero.verifier.npa.NPAValue;

/**
 * Strategy for reading and writing local variables during
 * non-pointer-array (NPA) verification.
 *
 * <p>Replaces {@code jx.zero.verifier.npa.NPALocalVarsInterface}.
 */
public interface LocalVarsStrategy {
    void write(int index, NPAValue type, int bcAddr);

    NPAValue read(int index);

    void setValue(NPAValue value, int newVal);
}
```

- [ ] **Step 2: Create `TypeCheckStrategy.java`**

```java
package jx.zero.verifier;

import jx.zero.verifier.typecheck.TCTypes;

/**
 * Strategy for reading and writing local variables during type-check
 * verification.
 *
 * <p>Replaces {@code jx.zero.verifier.typecheck.TCLocalVarsInterface}.
 */
public interface TypeCheckStrategy {
    void write(int index, TCTypes type, int bcAddr);

    TCTypes read(int index, TCTypes type);
}
```

- [ ] **Step 3: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/jx/zero/verifier/LocalVarsStrategy.java src/jx/zero/verifier/TypeCheckStrategy.java
git commit -m "feat(verifier): add LocalVarsStrategy and TypeCheckStrategy interfaces"
```

---

### Task 6: `AbstractVerifier` Base Class

**Files:**
- Create: `src/jx/zero/verifier/AbstractVerifier.java`

**Interfaces:**
- Consumes: legacy `VerifierInterface`, `jx.zero.ByteCode`, `jx.zero.classfile.MethodSource`, `Subroutines` (all in-package or already imported by `VerifierInterface`).
- Produces: `protected AbstractVerifier(MethodSource method, Subroutines srs, Object parameter)`, `public MethodSource getMethod()`, `public Subroutines getSrs()`, `public Object getParameter()`, `public final void runChecks()` (invokes `endChecks()` in a `finally` block so it runs even if a `checkBC` throws), `protected abstract ByteCode[] getByteCodes()` (must not return null), abstract `checkBC(ByteCode e)`, `getClassName()`, `endChecks()`.

- [ ] **Step 1: Create `AbstractVerifier.java`**

```java
package jx.zero.verifier;

import jx.zero.ByteCode;
import jx.zero.classfile.MethodSource;

/**
 * Base class for bytecode verifiers.
 *
 * <p>Holds the verifier's shared state (method, subroutine set, user
 * parameter) and drives the check lifecycle via
 * {@link #runChecks()}: iterate the bytecodes from
 * {@link #getByteCodes()}, {@link #checkBC(ByteCode)} each one, then call
 * {@link #endChecks()}.
 *
 * <p>Implements the legacy {@code VerifierInterface} for backward
 * compatibility.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractVerifier implements VerifierInterface {
    protected final MethodSource method;
    protected final Subroutines srs;
    protected final Object parameter;

    protected AbstractVerifier(MethodSource method, Subroutines srs, Object parameter) {
        this.method = method;
        this.srs = srs;
        this.parameter = parameter;
    }

    public MethodSource getMethod() {
        return method;
    }

    public Subroutines getSrs() {
        return srs;
    }

    public Object getParameter() {
        return parameter;
    }

    public final void runChecks() {
        try {
            for (ByteCode code : getByteCodes()) {
                checkBC(code);
            }
        } finally {
            endChecks();
        }
    }

    /**
     * Returns the bytecodes of the method to verify.
     *
     * <p>Must not return {@code null}; an empty array is allowed.
     */
    protected abstract ByteCode[] getByteCodes();

    public abstract void checkBC(ByteCode e);

    public abstract String getClassName();

    public abstract void endChecks();
}
```

- [ ] **Step 2: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/jx/zero/verifier/AbstractVerifier.java
git commit -m "feat(verifier): add AbstractVerifier base class"
```

---

### Task 7: Deprecate the 9 Legacy Hierarchy Interfaces

**Files:**
- Modify: `src/jx/devices/Device.java`
- Modify: `src/jx/devices/Bus.java`
- Modify: `src/jx/devices/ide/IDEDevice.java`
- Modify: `src/jx/devices/net/NetworkDevice.java`
- Modify: `src/jx/devices/pci/PCIDevice.java`
- Modify: `src/jx/fs/FileSystemInterface.java`
- Modify: `src/jx/zero/verifier/VerifierInterface.java`
- Modify: `src/jx/zero/verifier/npa/NPALocalVarsInterface.java`
- Modify: `src/jx/zero/verifier/typecheck/TCLocalVarsInterface.java`

**Interfaces:**
- Consumes: the new bases/capabilities/strategies from Tasks 1-6.
- Produces: the 9 listed interfaces marked `@Deprecated` with `@deprecated` javadoc naming the replacement. No signatures change.

- [ ] **Step 1: Deprecate `Device.java`**

Replace the existing javadoc + declaration (currently lines 3-7) with:

```java
/**
 * The top-level device interface. All devices must implement this interface.
 * @author Michael Golm
 * @deprecated Extend {@link AbstractDevice} instead of implementing this interface.
 */
@Deprecated
public interface Device {
```

- [ ] **Step 2: Deprecate `Bus.java`**

Replace the body (currently lines 1-5) with:

```java
package jx.devices;

/**
 * @deprecated Extend {@link AbstractDevice} and compose capability interfaces
 *             ({@link PciCapable}, {@link BlockIOCapable}) instead of implementing this interface.
 */
@Deprecated
public interface Bus extends Device {
    public abstract Device getChild(int index);
}
```

- [ ] **Step 3: Deprecate `IDEDevice.java`**

Replace the javadoc-close + declaration (currently lines 12-14) with:

```java
 * Partitionsnamen.
 * @deprecated Extend {@link AbstractDevice} and compose {@link PciCapable} / {@link BlockIOCapable} instead.
 */
@Deprecated
public interface IDEDevice extends Bus, Portal {
```

- [ ] **Step 4: Deprecate `NetworkDevice.java`**

Replace the body (currently lines 1-6) with:

```java
package jx.devices.net;

import jx.zero.*;
import jx.fs.buffer.separator.NonBlockingMemoryConsumer;
import jx.devices.Device;

/**
 * @deprecated Extend {@link jx.devices.net.AbstractNetworkDevice} instead of implementing this interface.
 */
@Deprecated
public interface NetworkDevice extends Device, Portal {
```

(The constants and method declarations below the opening brace are unchanged.)

- [ ] **Step 5: Deprecate `PCIDevice.java`**

Replace the javadoc + declaration (currently lines 5-10) with:

```java
/**
 *
 * @author xuyi
 * @deprecated Extend {@link AbstractDevice} and implement {@link PciCapable} instead.
 */
@Deprecated
public interface PCIDevice extends Bus {
```

- [ ] **Step 6: Deprecate `FileSystemInterface.java`**

Replace the body (currently lines 1-3) with:

```java
package jx.fs;

/**
 * @deprecated Extend {@link jx.fs.AbstractFileSystem} instead of implementing this interface.
 */
@Deprecated
public interface FileSystemInterface extends jx.zero.Portal {
```

- [ ] **Step 7: Deprecate `VerifierInterface.java`**

Replace the body (currently lines 1-6) with:

```java
package jx.zero.verifier;

import jx.zero.ByteCode;
import jx.zero.classfile.MethodSource;

/**
 * @deprecated Extend {@link jx.zero.verifier.AbstractVerifier} instead of implementing this interface.
 */
@Deprecated
public interface VerifierInterface {
```

- [ ] **Step 8: Deprecate `NPALocalVarsInterface.java`**

Replace the body (currently lines 1-3) with:

```java
package jx.zero.verifier.npa;

/**
 * @deprecated Implement {@link jx.zero.verifier.LocalVarsStrategy} instead.
 */
@Deprecated
public interface NPALocalVarsInterface {
```

- [ ] **Step 9: Deprecate `TCLocalVarsInterface.java`**

Replace the body (currently lines 1-4) with:

```java
package jx.zero.verifier.typecheck;

/**
 * @deprecated Implement {@link jx.zero.verifier.TypeCheckStrategy} instead.
 */
@Deprecated
public interface TCLocalVarsInterface {
```

- [ ] **Step 10: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`. Deprecation warnings are now expected (e.g. `DeviceFinder.find()` returns deprecated `Device`, and the bridge classes in Tasks 2-4 reference deprecated types) — warnings are fine, errors are not.

- [ ] **Step 11: Verify 100% coverage of the deprecation set**

Run:
```bash
grep -rl "@Deprecated" src/jx/devices/Device.java src/jx/devices/Bus.java src/jx/devices/ide/IDEDevice.java src/jx/devices/net/NetworkDevice.java src/jx/devices/pci/PCIDevice.java src/jx/fs/FileSystemInterface.java src/jx/zero/verifier/VerifierInterface.java src/jx/zero/verifier/npa/NPALocalVarsInterface.java src/jx/zero/verifier/typecheck/TCLocalVarsInterface.java | wc -l
```
Expected: `9`.

- [ ] **Step 12: Commit**

```bash
git add src/jx/devices/Device.java src/jx/devices/Bus.java src/jx/devices/ide/IDEDevice.java src/jx/devices/net/NetworkDevice.java src/jx/devices/pci/PCIDevice.java src/jx/fs/FileSystemInterface.java src/jx/zero/verifier/VerifierInterface.java src/jx/zero/verifier/npa/NPALocalVarsInterface.java src/jx/zero/verifier/typecheck/TCLocalVarsInterface.java
git commit -m "refactor(api): deprecate legacy hierarchy interfaces"
```

---

### Task 8: Composition Notes on Leaf Types

**Files:**
- Modify: `src/jx/devices/bio/BlockIO.java`
- Modify: `src/jx/devices/Keyboard.java`
- Modify: `src/jx/devices/Mouse.java`
- Modify: `src/jx/devices/Screen.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: javadoc composition notes on the 4 leaf types. **No `@Deprecated`** — these remain canonical value types returned by the capability interfaces.

- [ ] **Step 1: Add composition note to `BlockIO.java`**

Insert the line `<p>Composition note: do not implement this interface directly. Expose a {@code BlockIO} via {@link jx.devices.BlockIOCapable#getBlockIO()} on an {@link jx.devices.AbstractDevice} subclass.</p>` into the existing javadoc, immediately after ` * Access to a block device.`:

```java
/**
 * Access to a block device.
 * <p>Composition note: do not implement this interface directly. Expose a {@code BlockIO}
 * via {@link jx.devices.BlockIOCapable#getBlockIO()} on an {@link jx.devices.AbstractDevice} subclass.</p>
 * @author Michael Golm
 */
```

- [ ] **Step 2: Add javadoc to `Keyboard.java`**

Replace the body (currently lines 1-5) with:

```java
package jx.devices;

import jx.zero.Portal;

/**
 * Keyboard device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Keyboard}
 * via {@link InputCapable#getKeyboard()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Keyboard extends Portal {
    public void addKeyListener(KeyListener listener);
    public int getc();
    public int getcode();
}
```

- [ ] **Step 3: Add javadoc to `Mouse.java`**

Replace the body (currently lines 1-4) with:

```java
package jx.devices;

/**
 * Mouse device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Mouse}
 * via {@link InputCapable#getMouse()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Mouse extends jx.zero.Portal {
}
```

- [ ] **Step 4: Add javadoc to `Screen.java`**

Replace the body (currently lines 1-6) with:

```java
package jx.devices;

import jx.zero.DeviceMemory;
import jx.zero.Portal;

/**
 * Screen (display) device interface.
 *
 * <p>Composition note: do not implement this interface directly. Expose a {@code Screen}
 * via {@link DisplayCapable#getScreen()} on an {@link AbstractDevice} subclass.</p>
 */
public interface Screen extends Portal {
    public int getWidth();
    public int getHeight();
    public void moveCursorTo(int x, int y);
    public void putAt(int x, int y, char c);
    public void clear();
    public DeviceMemory getVideoMemory();
}
```

- [ ] **Step 5: Verify the build**

Run: `ant compile`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add src/jx/devices/bio/BlockIO.java src/jx/devices/Keyboard.java src/jx/devices/Mouse.java src/jx/devices/Screen.java
git commit -m "docs(api): add composition notes to leaf device interfaces"
```

---

### Task 9: Migration Guide

**Files:**
- Create: `docs/superpowers/guides/2026-08-07-ifos-abstract-base-migration.md`

**Interfaces:**
- Consumes: the entire new API surface from Tasks 1-6 and the deprecation map from Task 7.
- Produces: a written guide for sibling-project implementers.

- [ ] **Step 1: Create the guide**

```markdown
# ifOS Abstract Base Migration Guide

**Date:** 2026-08-07
**Applies to:** implementers in `APP`, `FS`, `GUI`, `HCI`, `NET`, `Simulator`, `WM` that `implements` `jx.devices.*`, `jx.fs.*`, or `jx.zero.verifier.*`.

## Why this exists

`ifOS` is the API/specification module. Its legacy interface forest
(`Device` -> `Bus` -> `PCIDevice` -> `Portal`) forced new implementations to
satisfy a 4-deep `implements` chain. The new API is:

- **one abstract base per subsystem** — the "what it IS" layer;
- **capability interfaces** — the "what it CAN DO" layer, composable and orthogonal.

Nothing is deleted. The legacy interfaces are `@Deprecated` but keep working,
so existing code compiles unchanged until you choose to migrate.

## New API surface (all in `src/jx`)

| Base class | Legacy interface it replaces | Capabilities to add |
|---|---|---|
| `jx.devices.AbstractDevice` | `Device`, `Bus` | `PciCapable`, `BlockIOCapable`, `InputCapable`, `DisplayCapable` |
| `jx.devices.net.AbstractNetworkDevice` | `NetworkDevice` | — |
| `jx.fs.AbstractFileSystem` | `FileSystemInterface` | — |
| `jx.zero.verifier.AbstractVerifier` | `VerifierInterface` | — |

Strategy interfaces (verifier): `jx.zero.verifier.LocalVarsStrategy` replaces
`NPALocalVarsInterface`; `jx.zero.verifier.TypeCheckStrategy` replaces
`TCLocalVarsInterface`.

## Pattern: before vs after

Before — a PCI device had to satisfy `Device`, `Bus`, `PCIDevice`, `Portal`:

```java
public class MyPCIDevice implements Device, Bus, PCIDevice, Portal {
    public DeviceConfigurationTemplate[] getSupportedConfigurations() { ... }
    public void open(DeviceConfiguration conf) { ... }
    public void close() { ... }
    public int getId() { ... }
    public Device getChild(int index) { ... }
    // ... every PCIDevice method
}
```

After — one class + capabilities. Move the body of `open()` into `init()`
and keep `getSupportedConfigurations()`:

```java
public class MyPCIDevice extends AbstractDevice implements PciCapable, BlockIOCapable {
    public MyPCIDevice() {
        super(0); // or super(config)
    }

    protected void init(DeviceConfiguration conf) {
        // real device setup (was the body of open())
    }

    protected void validateConfig(DeviceConfiguration conf) {
        // optional
    }

    public DeviceConfigurationTemplate[] getSupportedConfigurations() {
        // unchanged
    }

    public PCIAccess getPciAccess() { ... }
    public BlockIO getBlockIO() { ... }
}
```

Because `AbstractDevice` implements `Device`, the migrated instance still
passes `instanceof Device` and can be handed to code typed against the old API.

## Migration steps for an existing implementation

1. Change `implements X, Y, Z` to `extends AbstractDevice` (or
   `AbstractNetworkDevice` / `AbstractFileSystem` / `AbstractVerifier`).
2. Move the body of your `open(...)` method into `protected void init(DeviceConfiguration conf)`.
3. Delete your `getId()`/`close()` if they only returned a stored id / released a
   field — the base provides them. Keep a custom `close()` if you release
   OS resources.
4. Add capability interfaces (`implements PciCapable, ...`) and implement
   their accessor methods.
5. Compile. Fix any missing override that the base leaves abstract.

## Deprecation map (9 interfaces)

| Deprecated interface | Replace with |
|---|---|
| `jx.devices.Device` | `AbstractDevice` |
| `jx.devices.Bus` | `AbstractDevice` + capability interfaces |
| `jx.devices.ide.IDEDevice` | `AbstractDevice` + `PciCapable` / `BlockIOCapable` |
| `jx.devices.net.NetworkDevice` | `AbstractNetworkDevice` |
| `jx.devices.pci.PCIDevice` | `AbstractDevice` + `PciCapable` |
| `jx.fs.FileSystemInterface` | `AbstractFileSystem` |
| `jx.zero.verifier.VerifierInterface` | `AbstractVerifier` |
| `jx.zero.verifier.npa.NPALocalVarsInterface` | `LocalVarsStrategy` |
| `jx.zero.verifier.typecheck.TCLocalVarsInterface` | `TypeCheckStrategy` |

## Not deprecated, not in scope

`BlockIO`, `Keyboard`, `Mouse`, `Screen` remain canonical value types (they are
the return types of the capability interfaces). `DeviceFinder`,
`DeviceConfiguration`, `DeviceConfigurationTemplate`, `jx.devices.pci.{PCI,
PCIAccess, PCIAddress, PCICap}`, the `jx.fs.buffer.*` / `jx.fs.db.*`
subsystems, and `jx.fs.{Node, FS, FileSystem, StatFS, FSAttribute, Permission,
VolumeManager}` are untouched. Deleting the deprecated interfaces is future
work for a separate cross-project plan, after all implementations migrate.

## Verification

```bash
cd <ifOS root>
ant compile     # BUILD SUCCESSFUL (deprecation warnings are expected)
ant jar         # BUILD SUCCESSFUL, dist/ifOS.jar updated
```

Never run plain `ant` — its `javadoc` target fails on pre-existing errors in
`src/java/**` (copied JDK sources).
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/guides/2026-08-07-ifos-abstract-base-migration.md
git commit -m "docs: add abstract base migration guide"
```

---

## Final Verification (run after Task 9)

- [ ] Run `ant compile` from the working root → `BUILD SUCCESSFUL`.
- [ ] Run `ant jar` → `BUILD SUCCESSFUL`; `ls -la dist/ifOS.jar` shows a fresh timestamp.
- [ ] Run `git status --short` in the working root and confirm the only staged/unstaged changes are the 10 new files, the 13 modified files (9 deprecated + 4 noted), and the migration guide — **no** changes under `../APP`, `../FS`, `../GUI`, `../HCI`, `../NET`, `../Simulator`, `../WM`, or `dist/`.
- [ ] Count new API files: `ls src/jx/devices/{AbstractDevice,PciCapable,BlockIOCapable,InputCapable,DisplayCapable}.java src/jx/devices/net/AbstractNetworkDevice.java src/jx/fs/AbstractFileSystem.java src/jx/zero/verifier/{AbstractVerifier,LocalVarsStrategy,TypeCheckStrategy}.java | wc -l` → `10`.
- [ ] Spot-check one success criterion from the spec: grep that `Device` is `@Deprecated` and `AbstractDevice.java` contains `implements Device, AutoCloseable`.

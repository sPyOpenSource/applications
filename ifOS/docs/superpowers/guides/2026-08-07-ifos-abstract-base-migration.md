# ifOS Abstract Base Migration Guide

**Date:** 2026-08-07
**Applies to:** implementers in `APP`, `FS`, `GUI`, `HCI`, `NET`, `Simulator`, `WM` that `implement` `jx.devices.*`, `jx.fs.*`, or `jx.zero.verifier.*`.

## Why this exists

`ifOS` is the API/specification module. Its legacy interface forest — a deep
`implements` chain (`Device` -> `Bus` -> `PCIDevice`), plus the separate
`Portal` marker — forced new implementations to satisfy many interfaces at
once. The new API is:

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
passes `instanceof Device`, so code typed against `Device` keeps working.
It is NOT `instanceof Bus`, `PCIDevice`, or `Portal` — the base implements
only `Device`; `Portal` is a separate marker — so code typed against those
must be retyped (e.g. a `PCIDevice` consumer loses
`getAddress()`/`getInterruptLine()`/`getChild()`).

## Migration steps for an existing implementation

1. Change `implements X, Y, Z` to `extends AbstractDevice` (or
   `AbstractNetworkDevice` / `AbstractFileSystem` / `AbstractVerifier`).
2. Move the body of your `open(...)` method into `protected void init(DeviceConfiguration conf)`.
3. Delete your `getId()`/`close()` if they only returned a stored id / released a
   field — the base provides them. Note that `super(config)` leaves `deviceId = 0`;
   if your old `getId()` returned a real stored id, pass it via `super(id)` (the
   `DeviceConfiguration` overload keeps 0). To release OS resources, override
   `protected void deinit()` instead of `close()` — the base `close()` calls
   `deinit()` and then releases `config`. If you must override `close()`, call
   `super.close()` so the config release is not silently skipped.
4. Add capability interfaces (`implements PciCapable, ...`) and implement
   their accessor methods.
5. Compile. Fix any missing override that the base leaves abstract.

### Verifier and filesystem implementers

`AbstractVerifier` implements `VerifierInterface` and now provides
`runChecks()`, `getMethod()`, `getSrs()`, and `getParameter()` as concrete;
a legacy implementer should delete its copies. It adds a new abstract
`getByteCodes()` that must be implemented. The base `runChecks()` invokes
`endChecks()` via `try`/`finally`, so `endChecks()` always runs, even on
abnormal exit.

`LocalVarsStrategy` (replaces `NPALocalVarsInterface`) renames `NPAread` to
`read`. `TypeCheckStrategy` (replaces `TCLocalVarsInterface`) keeps
`write`/`read`.

`AbstractFileSystem` (replaces `FileSystemInterface`) still requires
`mount()`/`unmount()`; its `close()` now throws `Exception` and delegates to
`unmount()`.

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

No capability interface supplies `getChild(int)`, so a migrated bus/bridge
that served `Bus` consumers has no equivalent — plan such consumers
accordingly.

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

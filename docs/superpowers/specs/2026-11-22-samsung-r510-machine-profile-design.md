# Samsung R510 Machine Profile — Design

## Goal

Make the existing x86 simulator present a Samsung R510–like machine identity while
continuing to boot the jxcore image successfully. The profile is hardcoded as the
default configuration; no machine selector, config file, or new profile class is
introduced.

## Decisions

- **Machine identity only.** Report a Core 2 Duo–class CPU through `CPUID`.
- **Keep RAM at 512 MB.** This value is already proven to boot the jxcore image
  (the guest stack reaches ~0x11700000). Changing it is out of scope.
- **Keep UEFI boot.** The R510 shipped with legacy BIOS, but switching boot paths
  would risk the working jxcore boot; the existing UEFI firmware stays.
- **Keep all peripherals and timing unchanged.** VGA, UART, IDE, floppy, USB,
  PIT, oscillator, and BIOS services are not modified.

## Component changes

### `Simulator/src/jCPU/x86/X86Amd64.java` — `cpuid()`

- Leaf 0: report vendor `GenuineIntel`:
  - EBX = `0x756E6547` (`"Genu"`)
  - EDX = `0x49656E69` (`"ineI"`)
  - ECX = `0x6C65746E` (`"ntel"`)
  - EAX (max leaf) remains 1.
- Leaf 1: EAX = `0x00000776` — stepping 6, model 23, family 6 (Penryn/Core 2 Duo
  class). EDX feature bits remain `(1 << 4) | (1 << 15)` (TSC, CMOV); no FPU,
  SSE, or MSR support is claimed.
- Leaves `0x80000000` and `0x80000001` remain unchanged.

No other source files are changed.

## Data flow and error handling

The guest executes `CPUID` exactly as before; the instruction continues to dispatch
through the existing `cpuid(int leaf, int subleaf)` switch. Only the returned
identity values differ. No new state, dependencies, or failure paths are added.

## Testing

- Update `Simulator/test/jCPU/x86/X86Amd64Test.testCpuidReportsOnlyImplementedFeatures`
  to assert the new vendor string and leaf-1 EAX value, while retaining the
  existing feature-bit assertions.
- Run the full x86 unit suite. Expected result: only the pre-existing stale
  `X86CoreTest.testRegisterConstantsAndAccess` failure remains.
- Boot the jxcore disk image headlessly using the existing `UefiBootRunner` /
  `DebugBootRunner` path. Success means execution still reaches
  `Terminate DomainZero initial thread.` and continues into guest initialization
  with no new `#DB` traps or panics.

## Out of scope

- Changing RAM size, boot firmware, oscillator, or device identities.
- Adding chipset, super-I/O, EC, keyboard/touchpad, or card-reader devices.
- Adding a machine-selector UI or a reusable profile class.

# Design: ARM Utility UART Peripheral

## Status
- Date: 2026-07-14
- Status: Approved
- Owner: opencode

## 1. Purpose
Provide a simple, utility-focused UART interface for the armOS simulator to allow the simulated CPU to print text to the host console and receive input.

## 2. Architecture
The system uses Memory-Mapped I/O (MMIO). A specific memory range is reserved for the UART. Accesses to this range are intercepted by the `VirtualMemorySpace` and dispatched to a `UartPeripheral` object.

### Components
- **`Peripheral` (Interface)**: Defines the standard contract for all simulator devices.
    - `byte read(int offset)`
    - `void write(int offset, byte value)`
- **`UartPeripheral` (Class)**: Implements the UART logic.
    - Maintains a `ConcurrentLinkedQueue<Byte>` for input.
    - Forwards `write` calls to `System.out`.
    - A background thread reads from `System.in` to populate the input queue.
- **`VirtualMemorySpace` (Modified)**: Acts as the MMIO dispatcher.
    - Maintains a `Map<AddressRange, Peripheral>` of registered devices.
    - Intercepts `readByte`/`writeByte` calls to check for device hits before falling back to `PhysicalMemorySpace`.

## 3. Data Flow

### Transmit (CPU -> Console)
`CPU` $\to$ `VirtualMemorySpace.writeByte(ADDR, val)` $\to$ `UartPeripheral.write(0, val)` $\to$ `System.out.print((char)val)`

### Receive (Console -> CPU)
`System.in` $\to$ `UartPeripheral` (Background Thread) $\to$ `Queue` $\to$ `VirtualMemorySpace.readByte(ADDR)` $\to$ `UartPeripheral.read(0)` $\to$ `CPU`

## 4. Implementation Details

### Address Mapping
The UART will be mapped to a configurable address (default: `0x10000000`).

### Concurrency
- The input buffer must be thread-safe (`ConcurrentLinkedQueue`) as it is populated by a background thread and read by the simulation thread.
- The `System.in` reader will run in a daemon thread to ensure it doesn't block simulator shutdown.

### Alignment & Constraints
- The UART responds only to byte-level access.
- Reads are non-blocking; returning `0` if no data is available.

## 5. Testing Plan
1. **Unit Test**: Directly invoke `VirtualMemorySpace` with a registered `UartPeripheral` and verify console output.
2. **Integration Test**: Run a small ARM binary that performs `STRB` to the UART address with a known string.
3. **End-to-End**: Verify that a loop of "Echo" (Read byte -> Write byte) works between the host terminal and the simulator.
